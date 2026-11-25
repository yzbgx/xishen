package com.xishen.path;

import net.minecraft.server.level.ServerPlayer;

/**
 * 神道等级效果的统一入口。
 *
 * <p>现在**全部留空**：等以后定每条神道每一阶给什么效果（加属性、给技能、给被动），
 * 就在对应的 case 里写。调用点有两个：
 * <ul>
 *   <li>刚激活（0 → 1 阶）</li>
 *   <li>升阶（N → N+1 阶）</li>
 * </ul>
 *
 * <p>约定：效果要么是"持续生效的被动"（在 {@link #applyPassive} 里按当前阶数重算），
 * 要么是"升阶瞬间的一次性奖励"（在 {@link #onTierUp} 里发）。
 */
public final class PathEffects {
	private PathEffects() {
	}

	/** 升阶/激活瞬间调用一次 */
	public static void onTierUp(ServerPlayer player, PathType type, int newLevel) {
		switch (type) {
			case SHU -> {
				// TODO 书神道第 newLevel 阶的一次性奖励
			}
			case YI -> {
				// TODO 医神道
			}
			case BING -> {
				// TODO 兵神道
			}
			case HUANG -> {
				// TODO 黄神道
			}
			case QING -> {
				// TODO 青神道
			}
			case QIAO -> {
				// TODO 巧神道
			}
			case YIX -> {
				// TODO 弈神道
			}
			case XI -> {
				// TODO 戏神道
			}
			case OU -> {
				// TODO 偶神道
			}
			case WU -> {
				// TODO 巫神道
			}
			case LI -> {
				// TODO 力神道
			}
			case BU -> {
				// TODO 卜神道
			}
			case DAO -> {
				// TODO 盗神道
			}
			case CHANG -> {
				// TODO 娼神道
			}
		}
	}

	/**
	 * 重算"持续生效的被动效果"：登录、重生、升阶、加点后调用。
	 * 内容留空；实现时建议先清掉上一次给的所有属性修饰符，再按当前阶数重新加，避免叠加。
	 */
	public static void applyPassive(ServerPlayer player) {
		// TODO 按 PathAttachments.get(player) 里 14 条神道的阶数，重新计算被动加成
	}
}
