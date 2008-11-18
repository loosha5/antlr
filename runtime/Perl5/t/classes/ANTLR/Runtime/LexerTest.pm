package ANTLR::Runtime::LexerTest;

use strict;
use warnings;

use base qw( Test::Class );
use Test::More;

use ANTLR::Runtime::Lexer;

sub test_new_stream :Test() {
    my $input = ANTLR::Runtime::ANTLRStringStream->new({ input => 'ABC' });
    my $lexer = ANTLR::Runtime::Lexer->new($input);
}

1;
