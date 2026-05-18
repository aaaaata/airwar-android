package edu.hitsz.util;

import android.content.Context;

import edu.hitsz.R;
//头像切换工具类
/*
 * 讲解重点：
 * AvatarUtil 把后端或本地保存的 avatarId 转换为 Android drawable 资源 id。
 * 登录、排行榜、联机房间、PK 结算等页面都可以复用这个工具类显示头像。
 */
public class AvatarUtil {

    private AvatarUtil() {
    }

    // 根据数字头像 id 返回对应 drawable，异常 id 默认使用头像 1。
    public static int getAvatarResId(int avatarId) {
        switch (avatarId) {
            case 1:
                return R.drawable.avatar_1;
            case 2:
                return R.drawable.avatar_2;
            case 3:
                return R.drawable.avatar_3;
            case 4:
                return R.drawable.avatar_4;
            case 5:
                return R.drawable.avatar_5;
            case 6:
                return R.drawable.avatar_6;
            default:
                return R.drawable.avatar_1;
        }
    }

    // 保留带 Context 的重载，方便不同调用处统一写法；当前实现不需要真正使用 context。
    public static int getAvatarResId(Context context, int avatarId) {
        return getAvatarResId(avatarId);
    }
}
