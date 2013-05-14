package com.github.abrarsyed.gmcp

class SourceRemapper
{
	def methods, fields, params;
	
	// starting whitespace and modifiers random stuff
	// return type
	// method name
	// param
	final String METHOD = /(?[ \t].*?) (.*?|void) (.*?)\(((.*?) (.*?), )*?\)/;
}
