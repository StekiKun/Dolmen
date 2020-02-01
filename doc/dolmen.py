# -*- coding: utf-8 -*-
"""
    pygments.lexers.dolmen
    ~~~~~~~~~~~~~~~~~~~~~~~

    Lexers for Dolmen lexer and parser descriptions

    :author: Stéphane Lescuyer
    :license: EPLV2.0
"""

import re

from pygments.lexer import RegexLexer, words, include
from pygments.token import *

__all__ = ['DolmenLexerLexer', 'DolmenParserLexer', 'DolmenReportsLexer']

java_keywords = (
    "false", "null", "true",	# technically reserved literals, not keywords
    "abstract", "continue", "for", "new", "switch",
    "assert", "default", "if", "package", "synchronized",
    "boolean", "do", "goto", "private", "this",
    "break", "double", "implements", "protected", "throw",
    "byte", "else", "import", "public", "throws",
    "case", "enum", "instanceof", "return", "transient",
    "catch", "extends", "int", "short", "try",
    "char", "final", "interface", "static", "void",
    "class", "finally", "long", "strictfp", "volatile",
    "const", "float", "native", "super", "while"
)

class DolmenLexerLexer(RegexLexer):
    """Pygment custom lexer for Dolmen lexer descriptions"""

    # Properties for inclusion in the local Pygments distribution
    name = "DolmenLexer"
    aliases = ['dolmenlexer', 'jl']
    filenames = ['*.jl']

    # We want '.' to include newlines, and \b as well
    flags = re.MULTILINE | re.DOTALL
    
    # Auxiliary reg. exps and keywords lists
    jl_keywords = (
        "as", "eof", "import", "orelse", "private", "public",
        "rule", "shortest", "static"
    )
    
    # Lexer rules 
    tokens = {
        'root': [
            # Comments
            include('_comments'),
            # Java actions
            (r'{', Generic, 'action'),
            # Literals
            (r'"', String, 'string'),
            (r'\'', String.Char, 'character'),
            (r'[0-9]+', Literal.Number),
            # Keywords and identifiers
            (words(jl_keywords, prefix=r'\b', suffix=r'\b'), Keyword),
            (r'[_a-zA-Z][_a-zA-Z0-9]*', Name.Function),
            # Operators
            (r'\+', Operator),
            (r'\*', Operator),
            (r'\?', Operator),
            (r'\^', Operator),
            (r'\|', Operator),
            (r'\#', Operator),
            (r'-', Operator),
            (r'_', Operator),
            (r'=', Operator),
            # Punctuation
            (r'\(', Punctuation),
            (r'\)', Punctuation),
            (r'\[', Punctuation),
            (r'\]', Punctuation),
            (r'<', Punctuation),
            (r'>', Punctuation),
            (r',', Punctuation),
            (r';', Punctuation),
            # Catch-all rule
            (r'.', Text)
        ],

        'action': [
            (r'}', Generic, '#pop'),
            (r'{', Generic, '#push'),
            # Comments
            include('_java_comments'),
            # Java keywords
            (words(java_keywords, prefix=r'\b', suffix=r'\b'), Generic.Emph),
            # Java Literals
            (r'"', Generic.Strong, 'java_string'),
            (r'\'', Generic.Strong, 'java_character'),
            # Catch-all rule
            (r'.', Generic)
        ],

        'string': [
            (r'"', String, '#pop'),
            (r'[^"\\]', String),
            (r'\\', String, 'escapeSequence')
        ],

        'character': [
            (r'[^\'\\]', String.Char, ('#pop', 'endCharacter')),
            (r'\\', String.Char, ('#pop', 'endCharacter', 'escapeSequence'))
        ],
        'endCharacter': [
            (r'\'', String.Char, '#pop')
        ],

        'escapeSequence': [
            (r'\\|\'|"|r|n|b|t|f', String, '#pop'),
            (r'[0-9]{3}', String, '#pop'),
            (r'u+[0-9a-fA-F]{4}', String, '#pop')
        ],
        
        # A state only used for sharing comments between partitions
        '_comments': [
            (r'/\*', Comment.Multiline, 'mlcomment'),
            (r'//.*?$', Comment.Singleline)
        ],

        'mlcomment': [
            (r'\*/', Comment.Multiline, '#pop'),
            (r'[^*]', Comment.Multiline),
            (r'\*', Comment.Multiline)
        ],

        'java_string': [
            (r'"', Generic.Strong, '#pop'),
            (r'[^"\\]', Generic.Strong),
            (r'\\', Generic.Strong, 'java_escapeSequence')
        ],

        'java_character': [
            (r'[^\'\\]', Generic.Strong, ('#pop', 'java_endCharacter')),
            (r'\\', Generic.Strong, ('#pop', 'java_endCharacter', 'java_escapeSequence'))
        ],
        'java_endCharacter': [
            (r'\'', Generic.Strong, '#pop')
        ],

        'java_escapeSequence': [
            (r'\\|\'|"|r|n|b|t|f', Generic.Strong, '#pop'),
            (r'[0-9]{3}', Generic.Strong, '#pop'),
            (r'u+[0-9a-fA-F]{4}', Generic.Strong, '#pop')
        ],
        
        # A state only used for sharing comments between partitions
        '_java_comments': [
            (r'/\*', Generic.Deleted, 'java_mlcomment'),
            (r'//.*?$', Generic.Deleted)
        ],

        'java_mlcomment': [
            (r'\*/', Generic.Deleted, '#pop'),
            (r'[^*]', Generic.Deleted),
            (r'\*', Generic.Deleted)
        ]
    }

class DolmenParserLexer(RegexLexer):
    """Pygment custom lexer for Dolmen parser descriptions"""

    # Properties for inclusion in the local Pygments distribution
    name = "DolmenParser"
    aliases = ['dolmenparser', 'jg']
    filenames = ['*.jg']

    # We want '.' to include newlines, and \b as well
    flags = re.MULTILINE | re.DOTALL
    
    # Auxiliary reg. exps and keywords lists
    jl_keywords = (
        "continue", "import", "private", "public", "rule",
        "static", "token"
    )
    
    # Lexer rules 
    tokens = {
        'root': [
            # Comments
            include('_comments'),
            # Java actions and arguments
            (r'{', Generic, 'action'),
            (r'\(', Generic, 'argument'),
            # Literals
            (r'"', String, 'string'),
            # Keywords, token names and identifiers
            (words(jl_keywords, prefix=r'\b', suffix=r'\b'), Keyword),
            (r'[A-Z][A-Z_0-9]*\b', Name.Decorator),
            (r'[_a-zA-Z][_a-zA-Z0-9]*', Name.Function),
            # Operators
            (r'\^', Operator),
            (r'\|', Operator),
            (r'=', Operator),
            # Punctuation
            (r'<', Punctuation),
            (r'>', Punctuation),
            (r'\[', Punctuation),
            (r'\]', Punctuation),
            (r',', Punctuation),
            (r';', Punctuation),
            # Catch-all rule
            (r'.', Text)
        ],

        'action': [
            (r'}', Generic, '#pop'),
            (r'{', Generic, '#push'),
            include('_java')
        ],

        'argument': [
            (r'\)', Generic, '#pop'),
            (r'\(', Generic, '#push'),
            include('_java')
        ],

        # A state only used for sharing between actions and arguments
        '_java': [
            # Comments
            include('_java_comments'),
            # Java keywords
            (words(java_keywords, prefix=r'\b', suffix=r'\b'), Generic.Emph),
            # Java Literals
            (r'"', Generic.Strong, 'java_string'),
            (r'\'', Generic.Strong, 'java_character'),
            # Holes
            (r'#[_a-z][_a-zA-Z0-9]*', Generic.Inserted),
            # Catch-all rule
            (r'.', Generic)
        ],

        'string': [
            (r'"', String, '#pop'),
            (r'[^"\\]', String),
            (r'\\', String, 'escapeSequence')
        ],

        'character': [
            (r'[^\'\\]', String.Char, ('#pop', 'endCharacter')),
            (r'\\', String.Char, ('#pop', 'endCharacter', 'escapeSequence'))
        ],
        'endCharacter': [
            (r'\'', String.Char, '#pop')
        ],

        'escapeSequence': [
            (r'\\|\'|"|r|n|b|t|f', String, '#pop'),
            (r'[0-9]{3}', String, '#pop'),
            (r'u+[0-9a-fA-F]{4}', String, '#pop')
        ],
        
        # A state only used for sharing comments between partitions
        '_comments': [
            (r'/\*', Comment.Multiline, 'mlcomment'),
            (r'//.*?$', Comment.Singleline)
        ],

        'mlcomment': [
            (r'\*/', Comment.Multiline, '#pop'),
            (r'[^*]', Comment.Multiline),
            (r'\*', Comment.Multiline)
        ],

        'java_string': [
            (r'"', Generic.Strong, '#pop'),
            (r'[^"\\]', Generic.Strong),
            (r'\\', Generic.Strong, 'java_escapeSequence')
        ],

        'java_character': [
            (r'[^\'\\]', Generic.Strong, ('#pop', 'java_endCharacter')),
            (r'\\', Generic.Strong, ('#pop', 'java_endCharacter', 'java_escapeSequence'))
        ],
        'java_endCharacter': [
            (r'\'', Generic.Strong, '#pop')
        ],

        'java_escapeSequence': [
            (r'\\|\'|"|r|n|b|t|f', Generic.Strong, '#pop'),
            (r'[0-9]{3}', Generic.Strong, '#pop'),
            (r'u+[0-9a-fA-F]{4}', Generic.Strong, '#pop')
        ],
        
        # A state only used for sharing comments between partitions
        '_java_comments': [
            (r'/\*', Generic.Deleted, 'java_mlcomment'),
            (r'//.*?$', Generic.Deleted)
        ],

        'java_mlcomment': [
            (r'\*/', Generic.Deleted, '#pop'),
            (r'[^*]', Generic.Deleted),
            (r'\*', Generic.Deleted)
        ]
    }

class DolmenReportsLexer(RegexLexer):
    """Pygment custom lexer for Dolmen reports"""

    # Properties for inclusion in the local Pygments distribution
    name = "DolmenReports"
    aliases = ['dolmenreports', 'reports']
    filenames = ['*.reports']

    # We want '.' to include newlines, and \b as well
    flags = re.MULTILINE | re.DOTALL
    
    # Noteworthy words used in location reports
    reports_words = (
        "File", "line", "characters", "Warning:"
    )
    severities = (
        "Error:", "Warning:", "Log:"
    )
    
    # Lexer rules 
    tokens = {
        'root': [
            # Literals
            (r'"[^"]*"', String),
            (r'\'[^\']+\'', String.Char),
            (r'[0-9]+', Literal.Number),
            # Keywords
            (words(reports_words, prefix=r'\b', suffix=r'\b'), Keyword),
            (r'Error:', Name.Exception),
            (r'Warning:', Name.Label),
            (r'Log:', Name.Other),
            # Operators
            (r'-', Operator),
            (r',', Operator),
            (r'\.\.\.', Operator),
            # Catch-all rule
            (r'.', Text)
        ]
    }

    
