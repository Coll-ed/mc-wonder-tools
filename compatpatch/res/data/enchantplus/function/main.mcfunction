# 本文件覆盖 NeoEnchant（mr_neoenchant）的 enchantplus:function/main
#
# 原因：该函数是 NeoEnchant 5.14.0 的**唯一** tick 函数，全文只有 magnet 一条命令：
#   execute as @a at @s as @e[type=minecraft:item,distance=..10]
#     if data entity @s {Age:0s} unless data entity @s Thrower
#     run tp @s @p[predicate=enchantplus:magnet]
# 即：把玩家 10 格内**本 tick 刚生成、且不是玩家丢出**的物品实体，直接瞬移到带 Magnet 的玩家身上。
#
# 这会打断"刚生成的物品实体"的交付过程，实测导致车万女仆的相机照片（=女仆的唯一数据副本）
# 在生成瞬间被抓走、女仆随之消失（"相机收女仆把相册吞了"）。
#
# 覆盖成空函数后 magnet 的行为彻底消失；其它功能不受影响（已用校验器确认该函数只含 magnet）。
# 详见 compatpatch 的 README「⑦ 禁用 NeoEnchant 的 Magnet 附魔」。
