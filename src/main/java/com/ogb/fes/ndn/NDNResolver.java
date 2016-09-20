package com.ogb.fes.ndn;

import java.util.HashSet;

public interface NDNResolver {

	public HashSet<String> getListElements();
	
	public long getElapsedTime();
}
