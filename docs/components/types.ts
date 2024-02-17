export type TierList = {
	S: TierListEntry[],
	A: TierListEntry[],
	B: TierListEntry[],
	C: TierListEntry[],
	D: TierListEntry[],
	groups: string[][],
};

export type TierListEntry = string | string[];
