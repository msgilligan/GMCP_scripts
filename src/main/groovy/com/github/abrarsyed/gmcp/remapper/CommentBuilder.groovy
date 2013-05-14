package com.github.abrarsyed.gmcp.remapper

import joptsimple.internal.Strings
import lombok.Data

import com.sun.istack.internal.NotNull

@Data
public class CommentBuilder
{
	private ArrayList<Param>	params
	@NotNull
	public String				comment

	public void addParameter(String name, String comment)
	{
		params.add(new Param(name, comment))
	}

	public void getString(int indentLevel)
	{
		// init stuff
		String indent = Strings.repeat(' ', indentLevel * 4)
		String prefix = " * "
		StringBuilder builder = new StringBuilder()

		// first line
		builder.append(indent).append("/**").append(System.lineSeparator())

		// comment
		builder.append(prefix).append(comment).append(System.lineSeparator())

		// params
		for (Param p : params)
			builder.append(prefix).append(p).append(System.lineSeparator())

		// last line
		builder.append(indent).append("*/").append(System.lineSeparator())
	}

	private class Param
	{
		public final String	name
		public final String	comment

		public Param(String name, String comment)
		{
			super()
			this.name = name
			this.comment = comment
		}

		public String toString()
		{
			return "@param " + name + " " + comment
		}
	}
}
