# 电商订单系统 (Order System)

一个功能完整、可一键 Docker 启动的电商订单后端系统，覆盖下单 → 支付 → 取消 → 超时自动取消完整链路。

## 技术栈

| 层面 | 技术 |
|------|------|
| 语言 | Java 17 (LTS) |
| 框架 | Spring Boot 3.x |
| 数据库 | MySQL 8.x |
| 缓存 / 延迟队列 | Redis (ZSet) |
| ORM | JPA / Hibernate |
| 安全 | Spring Security + JWT + BCrypt |
| 接口文档 | springdoc-openapi (Swagger) |
| 构建 | Maven |
| 测试 | JUnit 5 + Mockito + Awaitility |
| 容器化 | Docker + Docker Compose |

## 核心功能

- **商品管理**：CRUD + 参数校验 + Redis 缓存，缓存命中时零 SQL
- **下单 (乐观锁防超卖)**：`@Version` 乐观锁扣库存，并发场景下保证 `stock ≥ 0` 且扣减守恒，不超卖
- **支付**：条件更新 `WHERE status=PENDING`，原子操作防重复支付
- **取消 (幂等)**：条件更新 + 逐商品恢复库存，重复调用结果一致
- **超时自动取消**：Redis ZSet 延迟队列，定时轮询到期订单自动取消，已支付订单不会被误取消
- **用户认证**：BCrypt 密码加密 + JWT 无状态认证，下单 userId 来自 SecurityContext（不信任前端）
- **订单归属校验**：用户 A 不能操作用户 B 的订单，返回 403
- **全局异常处理**：统一 `ApiResponse<T>` 返回结构（code/message/data）

## 项目结构

```
order-system/
├── src/main/java/com/example/order/
│   ├── common/          # 统一返回、异常定义
│   ├── config/          # Security、缓存、Swagger 配置
│   ├── controller/      # REST 控制器
│   ├── dto/             # 请求/响应 DTO
│   ├── entity/          # JPA 实体
│   ├── repository/      # 数据访问层
│   ├── service/         # 业务逻辑
│   └── util/            # JWT 工具
├── src/test/            # 29 个测试用例
├── Dockerfile           # 多阶段构建 (Maven → JRE 17)
├── docker-compose.yml   # 一键启动 app + MySQL + Redis
└── README.md
```

## 本地开发

**前置条件**：JDK 17、MySQL 8、Redis 7

```bash
# 启动 MySQL 和 Redis（本地端口 3307 / 6379）
# 然后：
cd order-system
./mvnw spring-boot:run

# Swagger 文档：http://localhost:8080/swagger-ui.html
```

application.yaml 中 DB/Redis 使用默认值 `localhost:3307` 和 `localhost:6379`。

## Docker 一键启动

```bash
cd order-system
docker-compose up -d
```

三容器 (app + MySQL + Redis) 自动拉起，MySQL 健康检查通过后应用才启动。
访问 `http://localhost:8080`。

```bash
# 停止
docker-compose down
# 数据持久化：MySQL 数据保存在命名卷，down 后再 up 数据不丢
```

## API 概览

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/users/register` | 注册 | 无 |
| POST | `/users/login` | 登录，返回 JWT | 无 |
| GET | `/products` | 商品列表 | 需 JWT |
| POST | `/products` | 创建商品 | 需 JWT |
| GET | `/products/{id}` | 商品详情 (带缓存) | 需 JWT |
| PUT | `/products/{id}` | 更新商品 | 需 JWT |
| DELETE | `/products/{id}` | 删除商品 | 需 JWT |
| POST | `/orders` | 下单 | 需 JWT |
| POST | `/orders/{id}/pay` | 支付 | 需 JWT |
| POST | `/orders/{id}/cancel` | 取消 | 需 JWT |

## 运行测试

```bash
./mvnw test   # 29 个测试，覆盖下单/取消/库存/超时/注册登录等核心路径
```
