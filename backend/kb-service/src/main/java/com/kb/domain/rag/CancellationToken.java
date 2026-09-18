package com.kb.domain.rag;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 流式生成的协作式取消令牌。
 * <p>
 * 由调用方（SSE 控制器）在每次请求时创建，客户端断连时调用 {@link #cancel()}。
 * 下游模型层通过 {@link #onAbort(Runnable)} 注册"真正中断在途 HTTP 调用"的动作，
 * 保证用户断开后供应商侧的 token 生成也被尽快中止，而不是只在本服务丢弃 token。
 * </p>
 *
 * @author forever-king

 */
public class CancellationToken {

    /**
     * 永不取消的共享令牌：cancel/onAbort 均为空操作。
     * 必须免疫取消——它是跨请求共享单例，任何一处误调 {@link #cancel()}
     * 都不能污染其他同步链路的调用方。
     */
    private static final CancellationToken NONE = new CancellationToken() {
        @Override
        public void cancel() {
            // 共享不可取消令牌：忽略
        }
        @Override
        public void onAbort(Runnable action) {
            // 不存在中止语义
        }
    };

    private volatile boolean cancelled;

    /** 取消时需要执行的中止动作（如 abort 底层 SSE 连接），注册与触发可能跨线程 */
    private final List<Runnable> abortActions = new CopyOnWriteArrayList<>();

    /**
     * 请求取消。幂等：只生效一次；在注册动作之前取消时，新注册的动作会立即执行。
     */
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        for (Runnable action : abortActions) {
            runQuietly(action);
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 注册取消时的中止动作；若令牌已取消，动作立即执行（避免注册晚于取消而漏中止）。
     */
    public void onAbort(Runnable action) {
        if (action == null) {
            return;
        }
        if (cancelled) {
            runQuietly(action);
        } else {
            abortActions.add(action);
            // 双重检查：注册期间可能刚好发生取消
            if (cancelled) {
                runQuietly(action);
            }
        }
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // 中止动作失败不能影响取消主流程
        }
    }

    /** 永不会被取消的令牌（同步链路/未接入取消的调用方使用） */
    public static CancellationToken none() {
        return NONE;
    }
}
