// ============================================================================
// 智汇校园 · CampusBrain — Jenkins 声明式流水线
//
// 功能：拉代码 → 后端构建(编译+测试+打包) → 构建 4 个镜像 →
//       本机 docker-compose 部署 → 冒烟测试
//
// 前置条件（详见 docs/Jenkins-CICD-实施文档.md）：
//   1. Jenkins 节点装有 git + JDK17 + Maven + Docker + docker compose
//   2. Jenkins 凭据（Secret text）已创建，ID 见下方 credentials() 列表
//   3. Pipeline 任务选择 "Pipeline script from SCM"，指向本仓库，Script Path = Jenkinsfile
//
// 注意：测试自包含（H2 + MockBean），不需要中间件即可跑；只有部署阶段需要 Docker。
// ============================================================================

pipeline {
    // 若部署与构建不在同一台机器，可给构建/部署节点打 label 后改成 agent { label '...' }
    agent any

    // 每次构建的镜像标签 = Jenkins 构建号，保证部署总是使用本次构建产出的新镜像
    environment {
        IMAGE_TAG = "${BUILD_NUMBER}"

        // ===== 密钥：从 Jenkins 凭据读取（Secret text），避免明文入仓库/入日志 =====
        // 在 Jenkins → Manage Jenkins → Credentials → Global 创建同名 ID 的凭据
        JWT_SECRET             = credentials('campusbrain-jwt-secret')
        INTERNAL_SIGN_SECRET   = credentials('campusbrain-internal-sign-secret')
        MYSQL_ROOT_PASSWORD    = credentials('campusbrain-mysql-root-password')
        KB_MYSQL_ROOT_PASSWORD = credentials('campusbrain-kb-mysql-root-password')
        RABBITMQ_PASSWORD      = credentials('campusbrain-rabbitmq-password')
        MINIO_ROOT_PASSWORD    = credentials('campusbrain-minio-root-password')
        MINIO_ACCESS_KEY       = credentials('campusbrain-minio-access-key')
        DEEPSEEK_API_KEY       = credentials('campusbrain-deepseek-api-key')
        EMBEDDING_API_KEY      = credentials('campusbrain-embedding-api-key')
    }

    options {
        timestamps()                                   // 日志带时间戳
        buildDiscarder(logRotator(numToKeepStr: '20')) // 只保留最近 20 次构建
        disableConcurrentBuilds()                      // 同一任务不并发执行
    }

    stages {

        stage('① 拉取代码') {
            steps {
                // 使用 Pipeline 任务中配置的 Git 仓库地址与凭据
                checkout scm
            }
        }

        stage('② 后端构建（编译 + 测试 + 打包）') {
            steps {
                dir('backend') {
                    // backend/pom.xml 聚合了 common-auth / gateway / cas-service / kb-service，
                    // 一条命令产出全部可执行 boot jar（gateway-*.jar / cas-server-*.jar / kb-service-*.jar）
                    sh 'mvn -B clean package'
                }
            }
        }

        stage('③ 构建 Docker 镜像') {
            steps {
                dir('backend') {
                    // 三个后端 Dockerfile 均为"运行阶段"，COPY 的是第②步产出的 jar
                    // 构建上下文必须指向对应模块目录（Dockerfile 里的 target/ 是相对它的）
                    sh 'docker build -t gateway:${IMAGE_TAG}     -f gateway/Dockerfile     gateway/'
                    sh 'docker build -t cas-service:${IMAGE_TAG} -f cas-service/Dockerfile cas-service/'
                    sh 'docker build -t kb-service:${IMAGE_TAG}  -f kb-service/Dockerfile  kb-service/'
                }
                dir('frontend') {
                    // 前端 Dockerfile 是多阶段构建（node 编译 + nginx 托管），自带构建，宿主机无需 Node
                    sh 'docker build -t frontend:${IMAGE_TAG} .'
                }
            }
        }

        stage('④ 部署（本机 docker-compose）') {
            steps {
                dir('backend') {
                    // 合并 中间件编排(docker-compose.yml) + 业务服务编排(docker-compose.business.yml)
                    // TAG 注入本次镜像标签；密钥直接来自流水线环境，compose 自动插值
                    sh 'TAG="${IMAGE_TAG}" docker compose -f docker-compose.yml -f docker-compose.business.yml up -d'
                }
            }
        }

        stage('⑤ 冒烟测试') {
            steps {
                // 经 网关 → KB 的全链路健康检查（/api/v1/kb/health 在网关白名单内，无需登录）
                sh '''
                    for i in $(seq 1 30); do
                        if curl -sf http://localhost:8888/api/v1/kb/health >/dev/null 2>&1; then
                            echo "冒烟测试通过：网关 -> KB 链路正常"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "冒烟测试失败：网关或 KB 未在 60 秒内就绪"
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "✅ CI/CD 流水线成功。本次镜像标签: ${IMAGE_TAG}"
        }
        failure {
            echo "❌ CI/CD 流水线失败，请查看构建日志定位问题。"
        }
    }
}
