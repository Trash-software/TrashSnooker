# PLAN

* ~~录像: 瞄的打点/实际打点, 实际力量~~
* ~~AI: 准度稳定性~~
* ~~AI暂停~~
* ~~AI第一目标球预测~~
* AI防守定式
* ~~根据选手画出大概的走位范围~~
* 碰撞和响袋声音
* ~~晕进去的也该算进攻成功~~
* ~~AI斯诺克最后一颗红球时的自由球~~ 没出现过了
* ~~AI目标球太近时无法识别~~
* ~~AI解斯诺克和防守时考虑心理~~
* ~~回放上一杆之后计分板就没用了~~
* ~~AI防守吃袋角惩罚~~
* ~~AI不进攻袋口球~~
* ~~AI斯诺克归位~~
* ~~AI斯诺克考虑捡出的彩球~~ 可能解决了，也可能没有
* ~~优化AI防守角度loop~~ 不知道怎么优化
* ~~AI黑八开局选球~~
* ~~黑八和九球开球进球率~~
* AI发现不好防守之后重新考虑进攻
* ~~Bug: AI打彩球防守时~~
* ~~袋口重力变线~~
* ~~AI斯诺克开球加塞及准确度~~
* ~~选手特殊出杆习惯，如瞄左打右~~
* ~~贴库球瞄准线~~
* ~~AI决定对手犯规时是否复位~~
* ~~检查AI换选手后GamePlayStage~~
* ~~AI大超分后进攻权重增加~~
* ~~修改塞的效果~~
* ~~斯诺克争黑球出现问题~~
* ~~AI解斯诺克能力~~

* ~~解斯诺克成功率~~
* ~~记录界面优化~~
* ~~斯诺克判定无意识救球~~
* ~~斯诺克看得到目标球3次打不到直接输~~
* 斯诺克走蓝球点位时留向下角度
* ~~记录中直接列出所有比赛~~
* ~~最后秀一杆~~
* ~~AI喜欢磨球的程度~~
* ~~齿轮效应~~ 部分失败
* ~~AI把球按难度分级，如基本球，拼命球~~
* AI复位时考虑上一杆
* ~~AI进攻难度: 球离袋口的距离->tolerance angle~~
* ~~AI连攻带防~~
* ~~AI进攻权重修改：有下就打~~
* ~~二段出杆动画~~
* ~~AI总体心态，比如进攻失败或失误影响心态~~
* ~~斯诺克无法自动判断目标彩球时，让用户选~~
* 让分、~~让球~~
* ~~双线瞄球（球的宽度）~~
* ~~bug：最后一颗红球打进了但是犯规了~~
* ~~bug：黑八开进黑八~~ 修了，但未测试
* ~~AI处理死球~~
* AI处理死球时没有考虑正在进攻的球打进了会怎么样
* ~~所有球都停止运动后再去停止静止球的自旋~~
* ~~bug: 斯诺克误进彩球时罚分不对~~
* ~~生涯模式AI的状态、排名第一遥遥领先的人的effort~~
* 极限打点与中间打点的准确度
* ~~小力分离角与杆法旋转~~
* ~~生涯模式选球杆~~
* ~~未确定的bug: 播放出杆动画时拖动鼠标导致报错~~ 不准你拖了
* ~~AI进球难度、走位难度通过error tolerance来判断。可使用数值或难度分级~~ 换了思路， error tolerance没有实现
* ~~重摆手中球~~
* AI那个著名的三角函数问题
* ~~AI解斯诺克贴球~~
* ~~AI更新后技术差的选手基本不进攻了~~
* 黑八开球犯规对手要求重开
* 降低ai防守打最薄边的权重
* 齿轮效应仍需调试
* ~~斯诺克自由球直接遮挡~~
* ~~卫冕冠军应该是一号种子选手~~
* ~~AI解斯诺克选择行程短的路线~~
* AI大角度球是不是稍微太准了点
* ~~第一次开回放时没有球~~ 勉强修复
* ~~黑八消极开球~~
* ~~Standalone回放：包含playerPerson和cue~~ Cue没做
* ~~AI的小力中袋补偿~~
* 解球始终有点怪
* AI斯诺克超分的那一球，应加大进攻的权重
* AI的心理影响似乎有问题
* AI打斯诺克自由球时会直接做障碍
* ~~record记录额外信息，如第几轮~~
* AI在即将被超分时应该打高分彩球才对
* 一个不太容易修复的bug: 斯诺克犯规同时进彩球，捡起来的彩球没有计入自由球计算
* ~~未解之谜：career.json里错误记录球员的赛事单杆最高分~~
* ~~回放内也要加入MatchID等记录~~
* ~~AI解斯诺克连续失败时考虑轻贴（因为找不到AI为什么解不到球的原因，但怀疑和弧线有关）~~
* ~~中八AI自由球时优先考虑最难的活球~~
* ~~禁用按空格切换手的操作~~
* ~~预设球桌：品牌等~~
* ~~调整加点~~
* bug: 回放时中八和九球的剩余球都是上一杆的
* ~~设置界面~~
* ~~斯诺克AI开球模拟~~
* 动球碰撞现在不粘了，但是偶尔会弹错
* 一场球结束后加入是否关闭的对话框
* ~~出杆动画扭得太厉害了（可能是因为和帧率扯上了关系）~~
* 小半径的袋角弧线有可能overshoot
* ~~debug模式在AI击球时有bug~~
* ~~用debug模式手动放置白球故意为难AI，AI计算时白球起始位置不对~~
* ~~将复位选项加入游戏菜单~~
* ~~AI击球时某颗球莫名其妙动一下~~
* 重新开球
* 限时击球
* 预设球桌的背景色（考虑TableMetrics包含更多或更少东西）
* ~~撞袋角后的诡异旋转（应该只是因为没消除吸库的旋转）~~
* ~~继续比赛后球桌的牌子没了~~
* ~~每个球员对各种规则的能力值，以及兼容问题~~
* ~~旋转传递略鬼畜~~
* 电脑让杆
* ~~AI斯诺克开球必须走旁边~~
* ~~美式球桌角袋太深了，需要先修复底袋扇形绘制的问题~~
* 美式球桌中袋又太浅了
* ~~AI考虑小力变线的问题~~
* 考虑将CareerMatch和FastGame抽象成一个类似GameHost的东西
* ~~生涯升级之后回到主界面，主界面的cache没有刷新~~
* ~~开球失机应让对方打，不管开没开进~~ 没这bug
* ~~俺们滴嘎都是活雷锋~~
* ~~记录类成就重复显弹窗~~
* ~~复位之后没更新可用手~~
* GAIN_BY_SNOOKER成就可能有问题。没解决
* ~~AI走位直球惩罚~~
* ~~观察有没有下球的工具~~
* ~~生涯美式九球AI vs AI~~
* ~~关于-开发团队~~
* ~~成就弹框的背景色~~
* ~~各种图标~~
* ~~大力冲球的力太大了~~
* ~~齿轮效应高低杆的传递~~
* 中八偶发性AI不开球
* ~~解除陷入bug的选项~~
* ~~成就：斯诺克累计得分，斯诺克累计罚分~~
* ~~成就：九球白金九~~
* 规则：九球自由球传9不算
* ~~AI翻袋~~
* AI偷点
* 疑似bug：打黑八时先碰其他球后进黑八，判空杆犯规
* 中八AI不会解球，甚至连白球路线都不显
* 绪哥不用黑科技
* SQL记录中区分生涯模式与快速游戏
* AI翻袋进攻的数据库记录
* 单杆最高和147平分奖金
* ~~呲杆进球成就~~
* 中八让前和让中
* onDrag在下球观察模式下依旧是在调击球的角度
* ~~游戏时间统计~~
* ~~自动修正由于力度偏差导致AI加塞时打不准~~ 算了
* 中八开球顶袋进球算2颗过线
* ~~出杆计时~~
* ~~九球提前打进九号但犯规，不应算输~~
* ~~贴库球优化~~
* ~~AI球员加塞的偏好~~
* ~~斯诺克金球~~
* ~~斯诺克开球进球成就~~
* 击球增加熟练度
* 开球违例/失机后选择继续打/对手打/重开
* 生涯球员出杆直度
* AI贴库球/后斯诺准度下降
* bug：两颗球很近时判定为空杆
* 考虑加入Z轴
* ~~生涯模式创建时选择是否加入其他生涯球员~~
* ~~女子选手力量加点~~
* ~~总加点限制，100能力值导致AI的bug~~
* ~~100的出杆准确度/杆法控制会导致呲杆~~
* ~~导出视频的声音，没进视频反而直接放出来了~~ 删了
* 五分点训练
* ~~斯诺克大超分式AI不许防守~~
* ~~如果手和架杆一样好，优先用手~~
* 削弱AI白球贴库时的准度
* ~~AI连续进攻时考虑下下颗的难度~~ 无法
* ~~斯诺克对局统计中，统计单杆制胜/中盘/尾盘~~ 中八还没做
* ~~瞄准延长线记忆~~
* ~~自动换冲杆~~
* ~~生涯改名~~
* ~~读取设置项时的try catch~~
* bug: 生涯斯诺克赛事中，似乎没有统计玩家的直接对手的单杆最高分
* ~~AI打斯诺克似乎不会常规回球~~
* ~~bug: 快速游戏恢复上次游戏后，打完后UI不会刷新，还是能进去~~ 部分修复
* ~~数字化的实时心态值~~
* ~~通过准度/控球等推测AiPlayStyle~~
* AI防守加个贴库分
* ~~加长把/架杆套筒~~
* 种子选手一轮游虽然拿奖金但是不计入排名
