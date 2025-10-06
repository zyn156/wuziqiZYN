### 一、项目简介

本项目为一个五子棋安卓应用，实现了本地对战、AI人机对战·、本地储存复盘、联网排行榜几项功能。

### 二、运行环境

请确保电脑已安装以下环境

Android Studio（kotlin）

IntelliJ IDEA(Spring Boot 项目,JDK17)

mysql

### 三、安装步骤

![image-20251006112440809](C:\Users\20501\AppData\Roaming\Typora\typora-user-images\image-20251006112440809.png)

如图下载并解压 `code.zip` 后，三个文件夹及文件依次为spingboot,android与mysql文件。

![image-20251006112616591](C:\Users\20501\AppData\Roaming\Typora\typora-user-images\image-20251006112616591.png)

![image-20251006112634479](C:\Users\20501\AppData\Roaming\Typora\typora-user-images\image-20251006112634479.png)

其中spingboot与android的项目文件导入时需先点击进入文件夹，再选择与idea同级的项目文件夹。

其中springboot项目文件导入idea的时候注意将jdk选为17，否则将导致配置失败。

### 四、运行方式

导入所有文件后，MySQL / PostgreSQL启动服务,idea运行后实现联网功能，再启动运行android即可。

### 五、注意事项

若运行报错，请先安装依赖。

Android 项目请确保 Gradle 已同步。

### 六、Android 端项目结构与文件说明

####  主界面与游戏逻辑
**MainActivity.kt**：应用入口，提供模式选择（玩家对战 / AI 对战 / 排行榜 / 历史记录 / 对局回放）等菜单按钮。

**GameBoardView.kt**：自定义棋盘控件，负责绘制棋盘、处理用户触摸落子、判断胜负、AI 落子逻辑等。

**HistoryActivity.kt**：显示历史对局记录，支持从数据库加载每局的胜负与时间。

**ReplayActivity.kt**：实现对局复盘，按顺序回放棋步，支持暂停/播放/快进。

**RankingActivity.kt**：显示玩家胜率、积分等排行榜信息。

**RankingAdapter.kt**：RecyclerView 适配器，绑定排行榜数据与界面。

####  数据层 (Room 数据库)
**AppDatabase.kt**：Room 数据库配置入口，统一管理 Entity 和 Dao。

**GameDao.kt**：数据库访问对象，提供插入、查询、删除对局记录的方法。

**GameEntity.kt**：定义单局游戏的基本信息（如玩家、结果、时间等）。

**MoveEntity.kt**：定义每步棋的坐标、颜色等详细信息。

#### 网络层 (API 模块)
**ApiModels.kt**：定义与服务器交互的数据模型。

**ApiService.kt**：声明网络接口方法，例如上传对局记录、获取排行榜等。

**RetrofitClient.kt**：配置 Retrofit 实例，封装网络请求初始化逻辑。

#### xml布局文件

**activity_main.xml**：应用主界面布局，包含模式选择、排行榜、历史记录、复盘等功能入口按钮。

**activity_history.xml**：历史对局页面布局，用于展示本地数据库中保存的对局记录列表。

**activity_ranking.xml**：排行榜页面布局，使用 RecyclerView 显示玩家排名、胜率等统计信息。

**activity_replay.xml**：复盘界面布局，包含棋盘展示区与播放控制按钮，用于回放历史对局过程。

**item_ranking.xml**：排行榜单项布局，定义每个玩家在排行榜中的显示格式（如昵称、积分、名次）。

**dialog_set_nickname.xml**：单人模式下的弹窗布局，用于输入玩家昵称。

**dialog_set_pvp_nicknames.xml**：玩家对战模式下的弹窗布局，用于输入双方玩家昵称。

### 七、后端项目结构说明（Spring Boot）

**WuZiQiApplication.java**：Spring Boot 启动类，负责加载配置并启动服务。

**controller/GameRecordController.java**：接口控制器，定义对局记录、排行榜、历史查询等 REST API。

**service/GameRecordService.java**：业务逻辑层，封装游戏记录的保存、查询和统计逻辑。

**entity/AiGameRecord.java**：AI 对战记录实体类，对应数据库表 `ai_game_record`。

**entity/LocalGameRecord.java**：玩家对战记录实体类，对应数据库表 `local_game_record`。

**mapper/AiGameRecordMapper.java**：AI 对战记录的数据访问接口，定义查询与插入语句。

**mapper/LocalGameRecordMapper.java**：玩家对战记录的数据访问接口。

**dto/RecordGameRequest.java**：定义保存对局请求的数据结构。

**dto/RankingResponse.java**：定义排行榜返回数据结构。

**resources/application.properties**：配置文件，设置数据库连接、端口、MyBatis 参数等。

### 八、数据库设计说明（MySQL）

数据库名：`wuziqi_db`

####  1. 人机对战记录表 `ai_game_record`

记录玩家与 AI 对战统计（胜率、总场次等）

| 字段名        | 类型         | 说明                |
| ------------- | ------------ | ------------------- |
| `id`          | BIGINT       | 主键                |
| `player_name` | VARCHAR(50)  | 玩家昵称            |
| `piece_type`  | TINYINT      | 棋子类型（1黑 2白） |
| `win_count`   | INT          | 胜场数              |
| `lose_count`  | INT          | 败场数              |
| `total_count` | INT          | 总场次              |
| `win_rate`    | DECIMAL(5,2) | 胜率（百分比）      |
| `create_time` | DATETIME     | 创建时间            |
| `update_time` | DATETIME     | 更新时间            |

#### 2. 本地对战记录表 `local_game_record`

记录玩家对战统计（胜率、总场次等）

字段与 `ai_game_record` 相同，仅区分模式。