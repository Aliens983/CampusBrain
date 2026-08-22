# AGENTS.md — cas-module-appointment

Core business module: service management, booking, appointment audit (approve/reject), consultation, equipment.

## Complete File List

```
com.laoliu.cas.appointment
├── interfaces/
│   ├── controller/
│   │   ├── admin/
│   │   │   ├── ServiceAdminController.java          ← GET/POST /admin/service
│   │   │   └── ServiceStatusAdminController.java     ← POST /admin/service-status/audit/{pass,reject}
│   │   └── app/
│   │       ├── BookAppController.java                ← POST /book, /book/{room,equipment,consultation}
│   │       ├── ConsultationAppController.java
│   │       ├── EquipmentAppController.java
│   │       ├── ServiceAppController.java
│   │       ├── ServiceController.java                ← Bridge: GET /service
│   │       ├── ServiceStatusAppController.java
│   │       └── ServiceStatusController.java          ← Bridge: GET /service-status/user
│   └── dto/
│       ├── request/
│       │   ├── AuditRequest.java                     ← orderId, status(1=通过,2=拒绝), reason
│       │   ├── ServiceAddRequest.java
│       │   └── SpecializedBookingRequest.java
│       └── response/
│           ├── BookingDTO.java
│           ├── BookResultResponse.java
│           ├── ConsultantResponse.java               ← FAKE HARDCODED DATA
│           ├── EquipmentResponse.java                ← FAKE HARDCODED DATA
│           └── ServiceStatusResponse.java            ← NOTE: leaks to domain layer via BookingRepository
├── application/service/
│   ├── BookService.java / impl/BookServiceImpl.java           ← booking create/cancel
│   ├── ConsultationService.java / impl/ConsultationServiceImpl.java  ← FAKE data
│   ├── EquipmentService.java / impl/EquipmentServiceImpl.java        ← FAKE data
│   ├── ServiceService.java / impl/ServiceServiceImpl.java            ← service CRUD
│   └── ServiceStatusService.java / impl/ServiceStatusServiceImpl.java ← audit logic + email
├── domain/
│   ├── entity/
│   │   ├── AppointmentRecord.java                     ← ANEMIC
│   │   └── Service.java                               ← ONLY entity with domain behaviors
│   └── repository/
│       ├── BookingRepository.java
│       └── ServiceRepository.java
└── infrastructure/persistence/
    ├── dataobject/
    │   ├── AppointmentRecordDO.java
    │   ├── ItemDO.java
    │   └── ServicesDO.java
    ├── mapper/
    │   ├── ItemMapper.java
    │   └── ServiceMapper.java
    └── repository/
        ├── BookingRepositoryImpl.java
        └── ServiceRepositoryImpl.java
```

## Key Business Logic

### BookService — Booking Flow
```
bookService(userId, serviceIds)
  → Validate serviceIds not empty
  → ServiceRepository.selectByIds(serviceIds)
  → Filter available (serviceState=1) vs unavailable
  → @Transactional → BookingRepository.insertServices(userId, availableIds)
  → Return BookResultResponse(success=[...], failed=[...])
```

Specialized bookings (room/equipment/consultation) all delegate to the same `bookService()`.
Cancel: `cancelBookings(userId, bookingIds)` → batch updates manage_status to CANCELLED(3).

### ServiceStatusService — Audit Flow
```
auditPass(orderId, reason?)
  → getServiceStatusByOrderId() → check non-null
  → auditService(orderId, APPROVED(1), reason)
  → Compose email: service name + description + optional reason
  → sendAuditEmail(orderId, "预约审核通过通知", content)

auditReject(orderId, reason)  // reason REQUIRED
  → Validate reason not blank → throw AUDIT_REASON_REQUIRED
  → getServiceStatusByOrderId() → check non-null
  → auditService(orderId, REJECTED(2), reason)
  → Compose email: service name + description + rejection reason
  → sendAuditEmail(orderId, "预约审核未通过通知", content)
```

SQL guard: `UPDATE item SET manage_status=? WHERE order_id=? AND manage_status=0`
— only pending appointments can be audited.

### Service Entity — Domain Behaviors
```java
public boolean isAvailable() { return serviceState != null && serviceState == 1; }
public void disable() { this.serviceState = 0; }
public void enable() { this.serviceState = 1; }
```

## Known Issues
1. **ConsultationServiceImpl** and **EquipmentServiceImpl** return hardcoded fake data — no DB tables, no real data
2. **ServiceStatusResponse** (interfaces-layer DTO) is used in `BookingRepository` domain interface — LAYERING VIOLATION
3. **Duplicate controllers** — `ServiceStatusController` and `ServiceStatusAppController` are duplicates
4. **ServiceController** directly injects `ServiceRepository` — BYPASSES SERVICE LAYER
5. **No Bean Validation** on request DTOs
6. **AppointmentRecord entity is anemic** — no `approve()`, `reject()`, `cancel()` methods

## Database Tables
- `services` (service_id, service_name, service_describe, service_state, timestamps)
- `item` (order_id, user_id, service_id, manage_status, reason, timestamps)
  - manage_status: 0=待审核, 1=通过, 2=拒绝, 3=取消

## Mapper XML
- `ItemMapper.xml` — most complex mapper: multi-table JOINs, conditional updates, batch operations
- `ServiceMapper.xml` — service CRUD

## Dependencies
- Depends: `cas-module-system` (UserInfoApi), `cas-module-infra` (EmailService), `cas-framework`
- Does NOT depend: any other business module
