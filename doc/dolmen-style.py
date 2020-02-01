# -*- coding: utf-8 -*-
"""
    pygments.styles.dolmen
    ~~~~~~~~~~~~~~~~~~~~~~~

    A color theme used for Dolmen lexer and parser descriptions.
    It uses the Generic tokens to highlight Java semantic actions
    with a special background color. Other than that, it is
    similar to monokai.

    :license: EPLv2.0
"""

from pygments.style import Style
from pygments.token import Keyword, Name, Comment, String, Error, Text, \
     Number, Operator, Generic, Whitespace, Punctuation, Other, Literal

class DolmenStyle(Style):
    """
    Style specialized for Dolmen lexer and parser descriptions
    """

        # work in progress...

    background_color = "#f8f8f8"
    default_style = ""

    styles = {
        # No corresponding class for the following:
        #Text:                     "", # class:  ''
        Whitespace:                "underline #f8f8f8",      # class: 'w'
        Error:                     "#a40000 border:#ef2929", # class: 'err'
        Other:                     "#000000",                # class 'x'

        Comment:                   "italic #8f5902", # class: 'c'
        Comment.Multiline:         "italic #8f5902", # class: 'cm'
        Comment.Preproc:           "italic #8f5902", # class: 'cp'
        Comment.Single:            "italic #8f5902", # class: 'c1'
        Comment.Special:           "italic #8f5902", # class: 'cs'

        Keyword:                   "bold #204a87",   # class: 'k'
        Keyword.Constant:          "bold #204a87",   # class: 'kc'
        Keyword.Declaration:       "bold #204a87",   # class: 'kd'
        Keyword.Namespace:         "bold #204a87",   # class: 'kn'
        Keyword.Pseudo:            "bold #204a87",   # class: 'kp'
        Keyword.Reserved:          "bold #204a87",   # class: 'kr'
        Keyword.Type:              "bold #204a87",   # class: 'kt'

        Operator:                  "bold #ce5c00",   # class: 'o'
        Operator.Word:             "bold #204a87",   # class: 'ow' - like keywords

        Punctuation:               "bold #000000",   # class: 'p'

        # because special names such as Name.Class, Name.Function, etc.
        # are not recognized as such later in the parsing, we choose them
        # to look the same as ordinary variables.
        Name:                      "#000000",        # class: 'n'
        Name.Attribute:            "#c4a000",        # class: 'na' - to be revised
        Name.Builtin:              "#204a87",        # class: 'nb'
        Name.Builtin.Pseudo:       "#3465a4",        # class: 'bp'
        Name.Class:                "#000000",        # class: 'nc' - to be revised
        Name.Constant:             "#000000",        # class: 'no' - to be revised
        Name.Decorator:            "bold #5c35cc",   # class: 'nd' - to be revised
        Name.Entity:               "#ce5c00",        # class: 'ni'
        Name.Exception:            "bold #cc0000",   # class: 'ne'
        Name.Function:             "#000000",        # class: 'nf'
        Name.Property:             "#000000",        # class: 'py'
        Name.Label:                "bold #f57900",   # class: 'nl'
        Name.Namespace:            "#000000",        # class: 'nn' - to be revised
        Name.Other:                "bold #4eba06",        # class: 'nx'
        Name.Tag:                  "bold #204a87",   # class: 'nt' - like a keyword
        Name.Variable:             "#000000",        # class: 'nv' - to be revised
        Name.Variable.Class:       "#000000",        # class: 'vc' - to be revised
        Name.Variable.Global:      "#000000",        # class: 'vg' - to be revised
        Name.Variable.Instance:    "#000000",        # class: 'vi' - to be revised

        # since the tango light blue does not show up well in text, we choose
        # a pure blue instead.
        Number:                    "bold #0000cf",   # class: 'm'
        Number.Float:              "bold #0000cf",   # class: 'mf'
        Number.Hex:                "bold #0000cf",   # class: 'mh'
        Number.Integer:            "bold #0000cf",   # class: 'mi'
        Number.Integer.Long:       "bold #0000cf",   # class: 'il'
        Number.Oct:                "bold #0000cf",   # class: 'mo'

        Literal:                   "#000000",        # class: 'l'
        Literal.Date:              "#000000",        # class: 'ld'

        String:                    "#4e9a06",        # class: 's'
        String.Backtick:           "#4e9a06",        # class: 'sb'
        String.Char:               "#4e9a06",        # class: 'sc'
        String.Doc:                "italic #8f5902", # class: 'sd' - like a comment
        String.Double:             "#4e9a06",        # class: 's2'
        String.Escape:             "#4e9a06",        # class: 'se'
        String.Heredoc:            "#4e9a06",        # class: 'sh'
        String.Interpol:           "#4e9a06",        # class: 'si'
        String.Other:              "#4e9a06",        # class: 'sx'
        String.Regex:              "#4e9a06",        # class: 'sr'
        String.Single:             "#4e9a06",        # class: 's1'
        String.Symbol:             "#4e9a06",        # class: 'ss'

        # Generic class is used for Java actions/arguments:
        #   - Generic -> Text
        #   - Generic.Emph -> Keyword
        #   - Generic.Strong -> String
        #   - Generic.Deleted -> Comment
        #   - Generic.Inserted -> String.Escape
        Generic:                   "#000000 bg:#e8e8f8",        # class: 'g'
        Generic.Emph:              "bold #204a87",   # class: 'ge'
        Generic.Strong:            "#4e9a06",        # class: 'gs'
        Generic.Deleted:           "italic #8f5902", # class: 'gd'
        Generic.Inserted:          "bold #800080",   # class: 'gi'
        # unchanged
        Generic.Error:             "#ef2929",        # class: 'gr'
        Generic.Heading:           "bold #000080",   # class: 'gh'
        Generic.Output:            "italic #000000", # class: 'go'
        Generic.Prompt:            "#8f5902",        # class: 'gp'
        Generic.Subheading:        "bold #800080",   # class: 'gu'
        Generic.Traceback:         "bold #a40000",   # class: 'gt'
    }
