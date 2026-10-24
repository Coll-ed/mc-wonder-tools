/**
 * 类装载校验：用 {@code -Xverify:all} 把补丁类**真的装进 JVM**。
 *
 * 为什么需要它：mixin 注解写错、字节码不合法、或引用了不存在的类型时，
 * 光看源码和磁盘上的 class 文件都看不出来 —— 只有让 JVM 校验一次才发现得了。
 * 这里只 Load（不 Initialize、不执行 static 块），所以不需要完整的模组运行环境。
 *
 * 用法： java -Xverify:all -cp <补丁jar;依赖jar...> LoadCheck <类名...>
 */
public final class LoadCheck {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("用法: LoadCheck <类名...>");
            System.exit(2);
        }
        int fail = 0;
        for (String name : args) {
            try {
                Class<?> loaded = Class.forName(name, false, LoadCheck.class.getClassLoader());
                System.out.println("  [PASS] " + name + " 已装载并通过字节码校验"
                        + "（超类 " + loaded.getSuperclass().getName() + "）");
            } catch (Throwable t) {
                fail++;
                System.out.println("  [FAIL] " + name + " 装载失败: " + t);
            }
        }
        System.out.println(fail == 0
                ? "  [ALL PASS] " + args.length + " 个类全部装载通过"
                : "  [FAIL] " + fail + " / " + args.length + " 个类装载失败");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private LoadCheck() {
    }
}
