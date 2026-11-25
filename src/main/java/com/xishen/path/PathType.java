package com.xishen.path;

/**
 * 十四神道。
 *
 * <p>原书里的十四大道："书医兵黄青巧弈，戏偶巫力卜盗娼"。
 *
 * <p>等级规则（框架）：
 * <ul>
 *   <li>每条神道先攒**职业熟练度**（0 ~ 900）</li>
 *   <li>熟练度到 {@link #ACTIVATION_PROFICIENCY}(100) 时**激活**该神道（以后接"神道星邀请"）</li>
 *   <li>激活后每多 100 熟练度 = +1 阶，最高 {@link #MAX_LEVEL}(9) 阶</li>
 * </ul>
 */
public enum PathType {
	SHU("shu", "书", "Book", 0x8A6A3B),
	YI("yi", "医", "Medicine", 0x5FA05A),
	BING("bing", "兵", "Weapon", 0xA03A3A),
	HUANG("huang", "黄", "Yellow", 0xD8C040),
	QING("qing", "青", "Azure", 0x3FA0A0),
	QIAO("qiao", "巧", "Craft", 0xB08040),
	YIX("yix", "弈", "Board", 0x505050),
	XI("xi", "戏", "Opera", 0xB03070),
	OU("ou", "偶", "Puppet", 0x7070B0),
	WU("wu", "巫", "Shaman", 0x606060),
	LI("li", "力", "Strength", 0x907040),
	BU("bu", "卜", "Divine", 0x305080),
	DAO("dao", "盗", "Thief", 0x505050),
	CHANG("chang", "娼", "Courtesan", 0xA05060);

	/** 激活一条神道需要的熟练度 */
	public static final int ACTIVATION_PROFICIENCY = 100;

	/** 每提升一阶需要的熟练度（激活后再攒 100 升 1 阶） */
	public static final int PROFICIENCY_PER_LEVEL = 100;

	/** 满阶（由用户定为 9 阶） */
	public static final int MAX_LEVEL = 9;

	/** 熟练度上限：9 阶 = 900 */
	public static final int MAX_PROFICIENCY = MAX_LEVEL * PROFICIENCY_PER_LEVEL;

	public static final PathType[] ALL = values();
	public static final int COUNT = ALL.length;

	private final String id;
	private final String zhName;
	private final String enName;
	private final int color;

	PathType(String id, String zhName, String enName, int color) {
		this.id = id;
		this.zhName = zhName;
		this.enName = enName;
		this.color = color;
	}

	public String id() {
		return this.id;
	}

	public String zhName() {
		return this.zhName;
	}

	public String enName() {
		return this.enName;
	}

	public int color() {
		return this.color;
	}

	/** 翻译键：path.xishen.<id> */
	public String translationKey() {
		return "path.xishen." + this.id;
	}

	/** 这个熟练度算到第几阶（未激活 = 0） */
	public static int levelFor(int proficiency) {
		return Math.max(0, Math.min(MAX_LEVEL, proficiency / PROFICIENCY_PER_LEVEL));
	}

	/** 熟练度够不够激活 */
	public static boolean isActivated(int proficiency) {
		return proficiency >= ACTIVATION_PROFICIENCY;
	}

	public static PathType byId(String id) {
		for (PathType type : ALL) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return null;
	}
}
