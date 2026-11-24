package com.xishen.path;

/**
 * 十四神道。
 *
 * <p>原书里的十四大道："书医兵黄青巧弈，戏偶巫力卜盗娼"。
 * 这里只做框架：每条神道有 id、中英文名和代表色，等级/加点内容以后再加。
 *
 * <p>id 用拼音（"弈"和"医"同音，所以弈用 {@code yix} 区分）。
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

	/** 每条神道的最高等级（框架值，以后可调） */
	public static final int MAX_LEVEL = 10;

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

	public static PathType byId(String id) {
		for (PathType type : ALL) {
			if (type.id.equals(id)) {
				return type;
			}
		}
		return null;
	}
}
