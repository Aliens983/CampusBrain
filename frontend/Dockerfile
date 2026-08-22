# ============================================================
# 前端 Dockerfile — 多阶段构建
# 阶段1: Node 构建 Vue 应用
# 阶段2: Nginx 托管静态文件 + 反向代理
# ============================================================

# --- 构建阶段 ---
FROM node:18-alpine AS builder

WORKDIR /build

# 利用 Docker 缓存：先复制依赖文件
COPY package.json package-lock.json* ./

# 安装依赖
RUN npm ci --registry=https://registry.npmmirror.com

# 复制源码并构建
COPY . .
RUN npm run build

# --- 运行阶段 ---
FROM nginx:alpine

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
