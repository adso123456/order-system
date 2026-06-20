# 项目计划：电商订单系统

> 这是本项目的「活文档」与唯一事实来源。每完成一步、每改一次需求，都要回写这里。
> 新对话接手项目时，先完整读这份文件再动手。

**状态**：第一遍进行中
**创建于**：2026-06-19　**最后更新**：2026-06-20 14:00

> ⚠️ **对零基础的提醒**：第一遍 T1~T15 看起来是 15 个任务，实际是正常节奏下**好几个月**的工程量。这不是缺点，是现实——你在从零搭建一个完整后端系统。把"第一遍完成"当成一个真正的里程碑去庆祝，别做完就立刻冲第二遍。

---

## 1. 项目概览
- **一句话目标**：用「电商订单系统」这个实战项目，从零完整走一遍 Java 后端开发技术栈，产出可用于面试展示的后端项目。
- **要解决的问题 / 动机**：零基础起步，通过一个业务主线清晰的项目（商品 → 下单 → 支付 → 取消 → 超时），串联 Java 后端所有核心技术，避免"学了一堆知识点但连不起来"。
- **谁来用 / 使用场景**：学习者本人；兼作面试展示项目，证明具备独立搭建后端系统的能力。

## 2. 项目基础（Foundation）
- **技术栈 / 平台**：
  - 语言：Java 17（LTS）
  - 框架：Spring Boot 3.x
  - 构建：Maven
  - 数据库：MySQL 8.x
  - 缓存 / 延迟队列：Redis
  - ORM：JPA / Hibernate（第二遍部分模块换 MyBatis 对比）
  - 安全：Spring Security + JWT + BCrypt
  - 文档：springdoc-openapi (Swagger)
  - 测试：JUnit 5 + Mockito
  - 日志：SLF4J + Logback
  - 容器化：Docker + Docker Compose
  - 第二遍新增：RabbitMQ / Kafka、Redisson、Prometheus、GitHub Actions
- **范围 — 做什么**：
  - **第一遍（单体应用）**：商品 CRUD → 下单+库存一致性（乐观锁）→ 取消订单（幂等）→ 超时自动取消（Redis ZSet）→ 用户认证（JWT）→ 缓存/文档/测试/容器化
  - **第二遍（分布式进阶）**：消息队列异步解耦 → Redisson 生产级延迟队列 → 分布式锁 → 可观测性 → CI/CD → MyBatis 对比
- **范围 — 暂不做 / 以后再说**：
  - 前端页面（用 Postman / Swagger 调接口即可）
  - 真正的微服务拆分（只了解概念，不做实施）
  - 生产级云部署（K8s、云服务等）
  - 支付对接真实第三方（用模拟支付）
- **已知约束**：
  - 零基础起步，环境从零搭建
  - 代码质量要求能拿得出手（面试展示级）
  - 无已有代码，完全从零开始
  - 严格按照阶段顺序推进，前一步没通不进下一步
- **质量命令**：
  - 运行测试：`mvn test`
  - 完整构建：`mvn verify`
  - 启动应用：`mvn spring-boot:run`
  - Docker 启动：`docker-compose up -d`

## 3. 方向与方案（Direction）
- **整体思路 / 架构**：
  - 第一遍：经典单体分层架构，Controller（接收请求）→ Service（业务逻辑）→ Repository（数据访问）→ DB/Cache
  - 第二遍：在第一遍代码上重构，逐步引入消息队列、分布式组件，体会"为什么要加这些东西"
  - 包结构：`com.example.order` 下按功能模块分包（product / order / user），每个模块内含 controller / service / repository / entity / dto
- **关键技术选型**：
  | 选型 | 理由 |
  |------|------|
  | Spring Boot 而非手写 Servlet | 行业标准，开箱即用 |
  | JPA / Hibernate 而非 MyBatis | 零基础友好，先体验 ORM，第二遍再对比 MyBatis |
  | MySQL 而非 PostgreSQL | 国内企业最常用 |
  | Redis ZSet 做延迟队列 | 轻量，无需额外中间件，第一遍够用 |
  | JWT 而非 Session | 无状态，前后端分离主流方案 |
  | Docker Compose 而非 K8s | 学习曲线低，一键拉起开发环境 |
- **明确不用的东西**：
  - 不用 XML 配置 Spring（全部注解 + yml）
  - 不用 Lombok（不引入额外工具，先手写 getter/setter 理解原理）
  - 第一遍不用消息队列（复杂度留给第二遍）
- **待定 / 风险点**：
  - Redis ZSet 延迟队列的局限性（无重试、无 ACK），第二遍用 RabbitMQ 替代
  - 乐观锁在极高并发下的表现（本机模拟够用，不做真正压测）

## 4. 最终效果与验收标准（Success Criteria）
- **完成时的样子**：一个功能完整、有文档、有测试、能一键 Docker 启动的电商订单后端系统。第一遍跑通单体全流程，第二遍完成分布式改造。
- **必须跑通的场景（第一遍）**：
  - [ ] 能讲清一个 HTTP 请求从进入到返回经过哪些层
  - [ ] 商品 CRUD 全通，参数校验和异常处理生效
  - [ ] 下单是一个事务，并发不超卖（乐观锁）
  - [x] 取消订单幂等、库存正确恢复
  - [x] 订单超时能自动取消，支付后不误取消
  - [x] 登录用 JWT，接口按权限拦截
  - [ ] 接了缓存、写了测试、有 Swagger 文档
  - [ ] 能用 docker-compose 一键拉起整个项目
- **必须跑通的场景（第二遍）**：
  - [ ] 用消息队列做过异步解耦
  - [ ] 用 Redisson / 分布式锁解决过多实例并发
  - [ ] 项目有基础的日志、健康检查、监控
  - [ ] 配过 CI 自动测试和构建
  - [ ] 能说出单体和微服务各自适合什么场景
- **测试要求**：关键 Service 必须有单元测试（下单、取消、库存扣减）；覆盖率不做硬性要求，但核心业务路径必须覆盖。
- **交付方式**：代码推送到 GitHub 仓库（公开），README 写清项目说明和启动方式，兼作面试展示。

## 5. 实施步骤（Tasks）

> 约定：`[ ]` 待办　`[x]` 已完成。当前正在做的任务，在末尾标 `👈 当前`；全部做完后标 `✅ 已完成`。
> 粒度：一个任务以"改完后能独立验证"为准。

### 第一遍 · 单体应用

- [x] **T1** 环境搭建：装 JDK 17、VS Code、MySQL、Redis、Postman，配好 `JAVA_HOME`，装好 VS Code 的 Java 与 Spring 插件包，验证各组件可用
  - 文件：无（环境配置）
  - 依赖：无
  - 验证：`java -version` 输出 17；`mysql -u root -p` 可登录；`redis-cli ping` 返回 PONG；VS Code 已装 Extension Pack for Java（Red Hat）和 Spring Boot Extension Pack，能跑 Hello World

- [x] **T2** 创建 Spring Boot 项目骨架：Spring Initializr 建项目，配好 `application.yml`，建分层目录结构
  - 文件：`pom.xml`、`application.yml`、包结构 `controller/service/repository/entity/dto`
  - 依赖：T1
  - 验证：用 VS Code 命令面板（Ctrl+Shift+P）的 Spring Initializr 生成项目；`mvn spring-boot:run` 启动成功；`curl localhost:8080/hello` 返回 200

- [x] **T3** 商品模块 — Entity + Repository + 建表
  - 文件：`entity/Product.java`、`repository/ProductRepository.java`、`application.yml`（DDL 配置）
  - 依赖：T2
  - 验证：应用启动后数据库自动建表 `product`；Repository 的 `findAll()` 可查

- [x] **T4** 商品模块 — Service + Controller + DTO + 参数校验
  - 文件：`dto/ProductRequest.java`、`dto/ProductResponse.java`、`service/ProductService.java`、`controller/ProductController.java`
  - 依赖：T3
  - 验证：Postman 调通商品 5 个接口（增删改查+列表），传非法参数被 `@Valid` 拦截返回 400

- [x] **T5** 全局异常处理 + 统一返回结构
  - 文件：`common/ApiResponse.java`、`common/GlobalExceptionHandler.java`
  - 依赖：T4
  - 验证：传非法参数返回统一 JSON 结构 `{"code":400,"message":"...","data":null}`；访问不存在的商品返回统一 404 结构

- [x] **T6** 订单模块 — Entity + 建表 + 下单接口（含事务、乐观锁扣库存） ✅ 已完成
  - 文件：`entity/Order.java`、`entity/OrderItem.java`、`entity/OrderStatus.java`、`dto/OrderRequest.java`、`dto/OrderResponse.java`、`service/OrderService.java`、`controller/OrderController.java`、`repository/OrderRepository.java`、`common/InsufficientStockException.java`
  - 依赖：T5
  - 验证：正常下单成功；库存不足被拒（InsufficientStockException→400）；并发测试 20 线程抢 10 库存→定理验证通过（stock≥0 + 扣减守恒），乐观锁冲突由全局异常处理器捕获返回 409 "下单失败,请重试"

- [x] **T7** 模拟支付接口 ✅ 已完成
  - 文件：`entity/OrderStatus.java`（加 PAID/CANCELLED）、`entity/Order.java`（加 paidAt）、`dto/OrderResponse.java`（加 paidAt）、`repository/OrderRepository.java`（@Modifying 条件更新）、`service/OrderService.java`（payOrder）、`controller/OrderController.java`（POST /orders/{id}/pay）、`common/IllegalOrderStateException.java`、`common/GlobalExceptionHandler.java`（+IllegalOrderStateException→409）
  - 依赖：T6
  - 验证：PENDING→PAID 正常支付（paidAt 有值）；重复支付→409；不存在订单→404；CANCELLED 支付→409；条件更新（UPDATE WHERE status=PENDING）按影响行数判结果，0 行时 existsById 区分 404/409

- [x] **T8** 用户主动取消订单（条件更新 + 幂等 + 恢复库存） ✅ 已完成
  - 文件：`service/OrderService.java`（新增 `cancelOrder` 方法）、`controller/OrderController.java`（新增 `POST /orders/{id}/cancel`）
  - 依赖：T7
  - 验证：取消后订单状态变为 CANCELLED，库存恢复；重复调取消接口，结果一致（幂等）；已支付订单取消被拒绝

- [x] **T9** Redis 超时自动取消 ✅ 已完成
  - 文件：`pom.xml`（+spring-boot-starter-data-redis）、`application.yml`（Redis + order.timeout-seconds）、`service/OrderService.java`（placeOrder 写 ZSet）、`OrderSystemApplication.java`（@EnableScheduling）、`service/OrderTimeoutService.java`（@Scheduled 轮询 ZSet）、`service/OrderTimeoutServiceTest.java`
  - 依赖：T8
  - 验证：下单后超时未支付 → 自动 CANCELLED + 库存恢复；下单后立即支付 → 到期仍是 PAID 不被误取消；Redis ZSet 确认有记录

- [x] **T10** 用户模块 — 注册 + BCrypt 加密 + JWT 登录 ✅ 已完成
  - 文件：`entity/User.java`、`repository/UserRepository.java`、`dto/UserRegisterRequest.java`、`dto/UserLoginRequest.java`、`dto/UserResponse.java`、`dto/LoginResponse.java`、`service/UserService.java`、`controller/UserController.java`、`util/JwtUtil.java`、`config/SecurityConfig.java`、`common/DuplicateUsernameException.java`、`common/InvalidCredentialsException.java`
  - 依赖：T5（不依赖订单模块，可并行）
  - 验证：注册成功数据库密码为 BCrypt 密文($2a$10$...) √；重复用户名→409 "用户名已存在" √；登录成功返回 token+username+expiresInMs √；密码错误→401 "用户名或密码错误" √；用户不存在→同样 401(模糊化) √；引入 security 后旧接口仍可访问(permitAll) √

- [x] **T11** Spring Security 集成 — 鉴权 + 接口拦截 ✅ 已完成
  - 文件：`config/SecurityConfig.java`、`config/JwtAuthFilter.java`、`util/JwtUtil.java`（加 userId claim + validateAndGetUserId）、`service/OrderService.java`（userId 改从 SecurityContext 取 + 归属校验）、`dto/OrderRequest.java`（移除 userId）、`common/ForbiddenException.java`
  - 依赖：T10
  - 验证：无 token→401 统一 ApiResponse 格式 √；注册/登录免 token √；合法 token 正常访问 √；下单 userId 来自 SecurityContext(不信任前端) √；用户 A 取消用户 B 订单→403 "无权操作该订单" √；过期/乱码 token→401(不报 500) √

- [x] **T12** Redis 缓存商品详情 ✅ 已完成
  - 文件：`service/ProductService.java`（加缓存注解）、`config/CacheConfig.java`
  - 依赖：T5、T9（需要 Redis 已通）
  - 验证：第一次查商品有SQL第二次零SQL(日志行数增量证明)√；更新商品后缓存失效→重新走库拿到新数据 √；redis-cli 看到 product::93 可读JSON且有TTL(≈10分钟) √
	  - 🪤 踩坑：①自定义ObjectMapper未开activateDefaultTyping→LinkedHashMap反序列化失败 ②无参构造未注册JavaTimeModule→LocalDateTime序列化失败 ③最终方案：ObjectMapper.findAndRegisterModules()+activateDefaultTyping(LaissezFaireSubTypeValidator,NON_FINAL)

- [x] **T13** Swagger 接口文档 + 日志 ✅ 已完成
  - 文件：`pom.xml`（加 springdoc 依赖）、`config/SwaggerConfig.java`（可选）、`application.yml`（日志配置）
  - 依赖：T11（所有接口基本就绪）
  - 验证：访问 `http://localhost:8080/swagger-ui.html` 能看到所有接口并可在线调试；日志输出到控制台且格式清晰

- [x] **T14** 单元测试 — 下单、取消、库存扣减等关键 Service ✅ 已完成
  - 文件：`src/test/java/.../service/OrderServiceTest.java`、`ProductServiceTest.java` 等
  - 依赖：T11
  - 验证：`mvn test` 全部通过；关键业务路径有测试覆盖（正常流程 + 异常流程）

- [ ] **T15** Docker 容器化 — Dockerfile + docker-compose.yml 👈 当前
  - 文件：`Dockerfile`、`docker-compose.yml`
  - 依赖：T14
  - 验证：`docker-compose up -d` 一键启动 app + MySQL + Redis；Postman 调接口正常返回

### 第二遍 · 分布式进阶

- [ ] **T16** 引入消息队列 — 下单成功后异步发通知
  - 文件：`pom.xml`（加 RabbitMQ 依赖）、`config/RabbitMQConfig.java`、`service/OrderNotificationListener.java`
  - 依赖：T15（第一遍全部完成）
  - 验证：下单后日志显示异步收到通知消息；MQ 挂了不影响下单主流程（解耦验证）

- [ ] **T17** 用 RabbitMQ 延迟队列替换 Redis ZSet 超时取消
  - 文件：`config/RabbitMQConfig.java`（加死信队列配置）、移除 `OrderTimeoutService.java` 的 ZSet 轮询逻辑
	  - > 💡 **设计备忘**：cancelOrder 的幂等（条件更新 `WHERE status=PENDING`）和状态校验兜底（IllegalOrderStateException/ResourceNotFoundException 跳过）**原样复用**，只替换"延迟触发机制"（ZSet 轮询 → MQ 延迟/死信队列）。主动取消保持同步调用，超时取消走 MQ 消费者，**两者共用同一个 cancelOrder 方法**。
  - 依赖：T16
  - 验证：超时取消行为与 T9 一致，但不依赖定时轮询；延迟消息到时间自动触发取消

- [ ] **T18** Redisson 集成 + 分布式锁
  - 文件：`pom.xml`（加 Redisson 依赖）、`config/RedissonConfig.java`、`service/OrderService.java`（关键操作加分布式锁）
  - 依赖：T17
  - 验证：模拟多实例部署下，同一订单不会被重复处理；Redisson 锁有自动续期

- [ ] **T19** 可观测性 — Actuator + 健康检查 + 结构化日志
  - 文件：`pom.xml`（加 actuator 依赖）、`application.yml`（暴露端点）
  - 依赖：T18
  - 验证：`/actuator/health` 返回健康状态；日志为 JSON 结构化格式

- [ ] **T20** CI/CD — GitHub Actions 自动测试 + 构建镜像
  - 文件：`.github/workflows/ci.yml`
  - 依赖：T18
  - 验证：推送代码到 GitHub 后自动触发测试；测试通过后自动构建 Docker 镜像

- [ ] **T21** 选学 — 拿一个模块从 JPA 换成 MyBatis
  - 文件：`pom.xml`（加 MyBatis 依赖）、选一个模块改写为 MyBatis Mapper + XML
  - 依赖：T18
  - 验证：改写后的接口行为与改写前完全一致，能说出 JPA 和 MyBatis 的差异

**👈 当前进行到**：T15

## 6. 进度日志（Progress Log）

> 每完成一步追加一条，最新的放最上面。

- **2026-06-20** — T14 单元测试完成 + 发现并修复一个跨任务回归 bug | 新增 15 个测试(3 新文件+1 追加)。🪤 **踩坑:T11 归属校验无意间破坏了 T9 超时取消**——T11 给 cancelOrder 加了 getCurrentUserId() 归属校验(SecurityContext 取当前用户),但 T9 的 OrderTimeoutService 定时任务跑在后台线程没有登录态,调 cancelOrder → getCurrentUserId() 抛 ForbiddenException("未登录")→未捕获→定时任务崩→订单永不超时取消。T14 测试发现 OrderTimeoutServiceTest 5 次稳定挂,排查日志定位根因。**修法**:OrderService 抽 doCancel(package-private,无归属校验,仅同包 service 层可调),cancelOrder(public)先做归属校验再委托 doCancel,OrderTimeoutService 改调 doCancel。**教训:给已有方法加鉴权时,要全局搜调用方——不是所有调用方都有用户上下文**(定时任务、MQ消费者、内部调用等)。这也是面试能讲的真实故事:安全加固时如何避免破坏后台系统任务。同时 OrderTimeoutServiceTest 用 Awaitility 轮询断言替换裸 Thread.sleep(10000),避免时序测试偶发失败。全量 29/29 稳定通过。下一步:T15 Docker 容器化 | 验证：①pom.xml 加 springdoc-openapi-starter-webmvc-ui 2.8.8；②SecurityConfig permitAll 放行 Swagger 路径，但因 Spring Security 6.x 默认 MvcRequestMatcher 无法匹配静态资源(ResourceHttpRequestHandler)，追加 WebSecurityCustomizer.web.ignoring() 让 Swagger 静态资源完全绕过过滤器链；③新建 OpenApiConfig 配置 SecurityScheme bearer/JWT；④OrderService 三个关键方法各加 INFO 日志；⑤application.yaml 加 logging.pattern.console。curl 验证：/swagger-ui/index.html→200, /v3/api-docs→200, /orders(无token)→401(安全规则完好)。🪤 踩坑：改完 SecurityConfig 后 Swagger 仍 401——排查发现旧 Java 进程(PID 9904)一直占着 8080 没退，代码改动从未生效。杀掉重启后 web.ignoring() 才真加载，问题消失。教训：改配置类后先确认进程重启到位 | 下一步：T14 单元测试
- **2026-06-20** — T12 Redis 缓存商品详情完成 | 验证：@EnableCaching + RedisCacheManager(GenericJackson2JsonRedisSerializer JSON序列化,TTL 10分钟)；ProductService.findById @Cacheable(product::id)；update/delete @CacheEvict。日志增量证明缓存命中(两次查询仅1条SELECT)。redis-cli 确认 product::id 可读JSON且有TTL。🪤 缓存序列化踩坑：①自定义ObjectMapper未开activateDefaultTyping→LinkedHashMap反序列化失败 ②无参构造未注册JavaTimeModule→LocalDateTime序列化失败 ③最终方案 ObjectMapper.findAndRegisterModules()+activateDefaultTyping(LaissezFaireSubTypeValidator,NON_FINAL)。不影响T9的StringRedisTemplate(ZSet)用法 | 下一步：T13 Swagger 接口文档 + 日志
- **2026-06-20** — T11 Spring Security JWT 拦截 + 订单归属校验完成 | 验证：JwtAuthFilter(OncePerRequestFilter)从 Authorization Bearer 头取 token→JwtUtil.validateAndGetUserId 验证并解析 userId(存于 claim)→构造 UsernamePasswordAuthenticationToken 设入 SecurityContext；SecurityConfig 改 STATELESS + /users/register|login permitAll + 其余 authenticated + 自定义 entryPoint 返回 ApiResponse 格式 401 JSON + addFilterBefore JwtAuthFilter；JwtUtil 加 validateAndGetUserId(用 jjwt 0.12.x parser.verifyWith.parseSignedClaims.getPayload) + generateToken 加 userId claim；OrderRequest 移除 userId 字段(不信任前端)；OrderService.placeOrder 从 SecurityContext 取 userId；payOrder/cancelOrder 先 findById (不存在→404) 再校验 order.userId==当前用户 (不匹配→403 ForbiddenException) 后才执行条件更新 | 下一步：T12 Redis 缓存商品详情
- **2026-06-20** — T10 用户模块完成 | 验证：pom.xml 加 spring-boot-starter-security + jjwt 0.12.6(api/impl/jackson)；application.yml 加 jwt.secret(BASE64 随机 256-bit) + jwt.expiration-ms:86400000；User实体 users 表 username 唯一约束 password 密文 role 默认 USER；POST /users/register BCrypt 加密存库 重复用户名→409；POST /users/login 校验密码成功返回 LoginResponse(token+username+expiresInMs) 失败统一 401 "用户名或密码错误"(不区分用户不存在/密码错)；JwtUtil 用 jjwt 0.12.x 新版 API(subject/issuedAt/expiration/signWith)密钥从配置读不硬编码；SecurityConfig permitAll 放行所有接口(完整拦截规则留 T11)；自定义 InvalidCredentialsException 避开 Spring Security 同名 BadCredentialsException 命名冲突 | 下一步：T11 Spring Security 鉴权 + 接口拦截
- **2026-06-19** — T9 Redis 超时自动取消完成 ｜ 验证：pom.xml 加 spring-boot-starter-data-redis；application.yml 配 Redis(localhost:6379) + order.timeout-seconds:60；OrderService.placeOrder 下单后写 Redis ZSet(order:delay, score=deadline)；启动类加 @EnableScheduling；OrderTimeoutService @Scheduled(fixedRate=5s) 轮询 ZRANGEBYSCORE 捞出到期订单，逐个调 cancelOrder（复用 T8 的 cancelOrder 方法），捕获 IllegalOrderStateException/ResourceNotFoundException 正常跳过，处理完 ZREM 移除。支付不删 ZSet，靠 cancelOrder 状态校验兜底。测试：超时未支付 → 自动 CANCELLED + 库存恢复 √；立即支付 → 到期仍是 PAID 库存不变 √ ｜ 下一步：T10 用户模块 JWT ｜ 验证：POST /orders/{id}/cancel，OrderRepository.cancelOrder 条件更新 `UPDATE orders SET status='CANCELLED' WHERE id=? AND status='PENDING'`（JPQL 枚举常量，clearAutomatically=true）；ProductRepository.restoreStock 纯原子增 `SET stock=stock+? WHERE id=?`（不走 @Version 避免恢复时冲突）；@Transactional cancelOrder 先改状态再逐商品恢复库存；幂等保证：条件更新 0 行时 existsById 区分 404/409，仅影响 1 行才恢复库存。测试覆盖：正常取消（库存 8→10）√、重复取消（第二次 409 库存保持 10 不变成 12）√、取消已支付订单（409 库存不动）√、取消不存在订单（404）√。**cancelOrder 方法已抽成独立可复用方法，T9 超时取消直接复用** ｜ 下一步：T9 Redis 超时自动取消 ｜ 验证：POST /orders/{id}/pay，条件更新 `UPDATE SET status=PAID, paid_at=NOW WHERE id=? AND status='PENDING'` 原子操作，影响行数判结果（1→成功，0→existsById 区分 404/409）；PENDING→PAID 正常支付 paidAt 有值；重复支付→409；不存在→404；CANCELLED→409；OrderStatus 枚举加 PAID/CANCELLED；IllegalOrderStateException→409 ｜ 下一步：T8 用户主动取消订单
- **2026-06-19** — T6 订单模块完成 ｜ 验证：Order + OrderItem 实体建表（orders/order_item），POST /orders 下单接口（@Transactional 事务内查商品→扣库存→建订单明细），@Version 乐观锁扣库存，ObjectOptimisticLockingFailureException→409，InsufficientStockException→400；并发 20 线程抢 10 库存，定理验证通过（stock≥0 且扣减守恒）。**🪤 踩坑记录：乐观锁的并发测试不应该追求"恰好 N 次成功"，因为 MySQL REPEATABLE_READ 快照隔离下，前几个事务会在第一轮提交前读到相同旧版本，仅少数成功。乐观锁只保证不超卖，不保证能卖完——正确断言是"库存≥0 + 扣减守恒定理"。之前为了凑"恰好 10 成功"反复调时序（随机 jitter/提交间隔/flush），最后全回退了——测试是用来验证逻辑的，不能反过来改业务代码迎合测试预期。** ｜ 下一步：T7 模拟支付接口
- **2026-06-19** — T5 全局异常处理 + 统一返回结构完成 ｜ 验证：ApiResponse<T> (code/message/data) 统一包裹所有返回；ResourceNotFoundException→404 (HTTP 状态码=body code)；MethodArgumentNotValidException→400 收集全部字段错误；兜底 Exception→500 不暴露堆栈(仅 log.error) ｜ 下一步：T6 订单模块 Entity+建表+下单(事务+乐观锁)
- **2026-06-19** — T4 商品 Service + Controller + DTO + 校验完成 ｜ 验证：5 个接口全通（增删改查+列表）；@Valid 校验生效（空名→400/负价→400/负库存→400）；update 只动 name+price+stock，createdAt 不变；version 字段 @Version 自动初始化=0；查不存在 id 返回 500（已知，T5 收） ｜ 下一步：T5 全局异常处理 + 统一返回结构
- **2026-06-19** — T3 商品 Entity + Repository + 建表完成 ｜ 验证：Product entity (BigDecimal price + @Version version) 创建，ProductRepository 扫描到，`ddl-auto: update` 自动生成 product 表（decimal(10,2)/version 字段正确），DataSourceAutoConfiguration 排除已移除，order_system 库已建 ｜ 下一步：T4 商品 Service + Controller + DTO + 参数校验
- **2026-06-19** — T2 创建 Spring Boot 项目骨架完成 ｜ 验证：`./mvnw spring-boot:run` 启动成功（端口 8080），`curl localhost:8080/hello` 返回 "Hello, Order System!"；分层目录（controller/service/repository/entity/dto/common/config/security）已建；阿里云 Maven 镜像已配 ｜ 下一步：T3 商品模块 Entity + Repository + 建表
- **2026-06-19** — T1 环境搭建完成 ｜ 验证：Java 17.0.9 + JAVA_HOME 已配；VS Code 已装 Java/Spring 插件包；MySQL 8 + Redis 7 通过 Docker 运行（端口 3307/6379），连通性验证通过；Postman 已装；Maven 跳过（T2 后用 mvnw） ｜ 下一步：T2 创建 Spring Boot 项目骨架
- **2026-06-19** — 项目立项，PLAN.md 创建完成。尚未开始实施。

## 7. 变更记录（Changelog）

> 只有方向性变更才单独记一条；同方向的多次小调整合并进同一条。

- **2026-06-19** — 项目初始化，PLAN.md 创建 ｜ 原因：新项目立项 ｜ 影响：全部 21 个任务已定义
- **2026-06-19** — PLAN.md 三轮微调 ｜ 原因：① 加阶段标记"第一遍进行中"+ 零基础工作量提醒；② T6 验证降级：JMeter → 简单多线程，避免工具兔子洞；③ T6 验证补充认知要求：理解乐观锁冲突是预期行为而非 bug ｜ 影响：第 2 节状态、T6 验证描述
- **2026-06-20** — T14 测试发现 T11 归属校验破坏 T9 超时取消 ｜ 原因：cancelOrder 加 getCurrentUserId() 后定时任务无 SecurityContext 抛异常 ｜ 影响：OrderService 抽 doCancel；OrderTimeoutService 改调 doCancel
- **2026-06-19** — IDE 由 IDEA 改为 VS Code ｜ 原因：现成环境只有 VS Code，单体项目够用 ｜ 影响：T1、T2 验证标准更新

## 8. 完成复盘（Retrospective）

> 仅在项目状态变为「已完成」时填写。

（尚未完成）
