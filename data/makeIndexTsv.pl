#!/usr/bin/perl -w
use strict;

use Locale::Script;
use Locale::Language;

while( <> ) {
	/^(.*?):\s+<transform source="(.*?)"/ ;
	my ($path,$scriptIn) = ($1,$2);
	# print "Path: $path\n";
	print "$path\t";
	if( length($scriptIn) == 4 ) {
		my $script  = code2script($scriptIn);
		# print "ScriptIn: $script\n";
		print "$script\t";
	}
	elsif( length($scriptIn) < 3 ) {
		my $script  = code2language($scriptIn);
		# print "ScriptIn: $script\n";
		print "$script\t";
	}
	else {
		# print "ScriptIn: $scriptIn\n";
		print "$scriptIn\t";
	}

	/target="(.*?)"/;
	my $scriptOut = $1;
	if( length($scriptOut) == 4 ) {
		my $script  = code2script($scriptOut);
		# print "ScriptOut: $script\n";
		print "$script\t";
	}
	elsif( (length($scriptOut) <= 3) ) {
		my $script  = code2language($scriptOut);
		# print "ScriptOut: $script\n";
		if ( $script ) {
			print "$script\t";
		}
		else {
			print "$scriptOut\t";
		}
	}
	else {
		# print "ScriptOut: $scriptOut\n";
		print "$scriptOut\t";
	}
	if ( /variant="(.*?)"/ ) {
		my $variant = $1;
		# print "Variant: $variant\n";
		print "$variant\t";
	}
	else {
		print "\t";
	}

	/direction="(.*?)"/;
	my $direction = $1;
	# print "Direction: $direction\n";
	print "$direction\t";

	if( /alias="(.*?)"/ ) {
		my $alias = $1;
		# print "Alias: $alias\n";
		$alias =~ s/(.*?) (.*)/$1/;
		print "$alias\t";
	}
	else {
		print "\t";
	}

	if ( /backwardAlias="(.*?)"/ ) {
		my $backwardAlias = $1;
		# print "BackAlias: $backAlias\n";
		$backwardAlias =~ s/(.*?) (.*)/$1/;
		print "$backwardAlias\t";
	}
	else {
		print "\t";
	}
	 if ( /visibility="(.*?)"/ ) {
		my $visibility = $1;
		# print "Visibility: $visibility\n";
		print "$visibility\t";
	}
	else {
		print "\t";
	}
	print "\n";
}
