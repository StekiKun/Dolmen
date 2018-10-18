/**
 * This package contains the lexer and parser definitions
 * for the simple Dolmen lexer descriptions.
 * <p>
 * Simple Dolmen lexer descriptions are a subset of
 * full-blown (also known as <i>extended</i>) lexer
 * descriptions and the first stage in the boot-strapping
 * process of parsing Dolmen language descriptions with
 * Dolmen itself.
 * <p>
 * The lexer for simple lexer descriptions is generated
 * using Dolmen, but since no lexer or parser for lexer
 * descriptions exist at this point, its description is
 * constructed and provided declaratively in {@link jl.JLLexer}
 * instead of being described in .jl syntax. That class'
 * {@link jl.JLLexer#main(String[])} method can then be used
 * to turn that declarative grammar description into the
 * actual parser {@link jl.JLLexerGenerated}.
 * <p>
 * The parser for simple lexer descriptions is simply
 * written manually as a top-down parser, in {@link jl.JLParser}.
 * The flow and principle follows the one of a Dolmen-generated
 * parser, and the actual BNF grammar is included in Javadoc.
 * <p>
 * <i>NB: The process of generating {@link jl.JLLexerGenerated} 
 * is automated by the target {@code jllexer} of 
 * the {@code build.xml} ANT script.</i>
 */
@org.eclipse.jdt.annotation.NonNullByDefault package jl;