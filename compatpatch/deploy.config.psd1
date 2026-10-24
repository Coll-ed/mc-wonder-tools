@{
    # Compat Patch 部署配置 —— 机器相关路径只在本文件出现一次，脚本只引用符号。
    #
    # hardcode-ok: VersionsDir 是"部署到哪台机器的哪个目录"这一**部署目标本身**，它就是配置值；
    #              换机器/换整合包只需改这一行，代码里没有任何副本。
    VersionsDir = 'D:\GAME\MC\.minecraft\versions'

    # 实例目录名匹配片段（用 -like 匹配，避免把带中文的完整目录名再写死一遍）
    InstanceMatch = '*所留下的足迹*'

    # 要放进 mods 的补丁 jar 文件名
    PatchJarName = 'compatpatch-1.0.0.jar'

    # 放到 mods/disabled 下即等于"停用"（便于临时关掉而不删文件）
    DisabledDirName = 'disabled'

    # ── 构建用（与部署共用本文件，避免出现第二份路径副本）──

    # 本补丁 mixin 目标所在的模组 jar（用通配符匹配，避免写死版本号）
    TargetModJarPatterns = @(
        '*ScorchedGuns*.jar',
        'jupiter-*.jar',
        '*ice_and_fire_spellbooks*.jar',
        'vss-*.jar',
        'curios-*.jar',
        # ⑥ 的 mixin 目标（Wakes 是 Fabric 模组，经 Connector 加载；只给校验器用，不参与编译）
        '*wakes*.jar'
    )

    # 库目录（Minecraft 依赖 jar 的来源），相对实例目录。
    # 实例在 .minecraft\versions\<实例名>，而 libraries 在 .minecraft\libraries，所以上两级。
    LibrariesSubDir = '..\..\libraries'

    # 编译针对的 Minecraft 版本标签。
    # 必须与实例版本、以及 res/META-INF/neoforge.mods.toml 里 minecraft 的依赖范围一致。
    # 实例的 libraries 里同时存在 1.20.1 与 1.21.1 两份客户端 jar（前者是别的整合包留下的），
    # 两份都含 net/minecraft 的类，会解析到错版本 → 按本标签只保留目标版本那一份。
    MinecraftVersionTag = '1.21.1'

    # Minecraft 客户端 jar 在 libraries 里的路径片段（库目录布局，非本机特有）
    MinecraftClientJarDirFragment = '\net\minecraft\client\'

    # 编译输出与产物
    OutDir = 'build'
    ClassesSubDir = 'build\classes'

    # 离线校验器额外需要的 jar：③ 要对比「扩展模组期望的类型」与「Ice and Fire 真实字段类型」。
    # 它不参与编译（不需要写 mixin），所以不放进 TargetModJarPatterns。
    # ⑦ 要覆盖其数据的目标模组（纯数据包模组，无代码/无配置）
    NeoEnchantJarPattern = 'NeoEnchant-*.jar'

    IceAndFireJarPattern = '*iceandfire-*.jar'

    # 校验器源码与编译产物
    VerifierSource = 'tools\VerifyCompatPatch.java'
    VerifierOutDir = 'build\verify-tools'

    # 编译用 JDK。优先级：环境变量 JAVA_HOME > 本值。
    # hardcode-ok: 这是"本机装了哪个 JDK"的环境事实，配置文件的职责就是记录它；
    #              代码里没有任何副本（build.ps1 只引用本符号），换机器改这一行即可。
    JdkHome = 'C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot'
}
