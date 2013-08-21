Oscar3 alpha 5
--------------

Oscar3 is still under development, and so here are some instructions to get you 
started. Further documentation is available in the doc directory (if you
have received Oscar3 as source code, then this will be produced during the
build process) - this contains the Oscar3 Javadoc, and a link to additional 
HTML documentation. This HTML documentation can also be accessed via the Oscar3
server (see below).

Included within Oscar3 is SAPIENT: "Semantic Annotation of Papers: Interface & 
ENrichment Tool". This has been developed by Liakata et al. at the University of
Wales, Aberystwyth. More information about SAPIENT and how to run both within Oscar3 or 
as a standalone can be found in:

SAPIENT_FAQ.txt and src/uk/ac/aber/art_tool/art_tool_web/SAPIENT_FAQ.html

If you find Oscar3 useful, then please send me mail (to peter.corbett@cantab.net),
subscribe to the mailing list at:

https://lists.sourceforge.net/lists/listinfo/oscar3-chem-announce

You can also read my blog at:

http://wwmm.ch.cam.ac.uk/blogs/corbett/

If you use Oscar3 to produce results for publication, then please cite us:

High-Throughput Identification of Chemistry in Life Science Texts
Peter Corbett and Peter Murray-Rust
CompLife 2006, LNBI 4216, pp. 107-118, 2006.

Semantic enrichment of journal articles using chemical named entity recognition
Colin R. Batchelor and Peter T. Corbett
Proceedings of the ACL 2007 Demo and Poster Sessions, pages 45?48,
Prague, June 2007.

If you use SAPIENT to produce results for publication, then please cite :

Semantic Annotation of Papers: Interface and Enrichment Tool (SAPIENT).
Liakata M., Claire Q and Soldatova L. N. (2009)  
Proceedings of BioNLP 2009, p. 193--200, Boulder, Colorado.

A few disclaimers:

1) Oscar3 is under development. As such, it will produce more warnings and 
   print out more exceptions than production code. This doesn't necessarily 
   mean there's a big problem - look at the output files that are being
   generated to see if it's performing OK before mailing me bug reports.
2) NO WARRANTY. In particular, it is important to realise that a) Natural 
   Language Processing is a hard problem, and it is very rare to get anything
   to be 100% accurate, b) current development of Oscar3 is based on the idea
   that there will be further stages of processing (automatic or manual) of the
   results, and c) Oscar3 is still under development. Don't place too much 
   trust in the results at this stage.
3) It takes a long time for certain parts of Oscar3 to start up. This makes it 
   slow to process single papers. When acting in server mode, the relevant
   modules are initialised as and when they are needed - so often there will 
   be a 30-second or longer delay while the modules initialise.

Instructions:

1) If you've downloaded the .zip containing oscar3-a5.jar, then go to step 2.

If you've got the project in source code form, run ant (the Java 'make'
replacement)

ALTERNATIVELY (for Java programmers) import the source tree into your IDE, 
put all of the included .jars on the build path and build in the normal way.
The class you want to run is uk.ac.cam.ch.wwmm.oscar3.Oscar3.

2) Get a test document. A good example can be found here:

http://www.rsc.org/delivery/_ArticleLinking/ArticleLinking.cfm?JournalCode=OB&Year=2006&ManuscriptID=b515385a&Iss=3

(Save it as test.html somewhere)

4) Try processing the test file: 

java -Xmx512m -jar oscar3-a5.jar Process test.html test.xml

Don't worry about any exceptions or warnings at this stage - see the
disclaimers above.

5) Take a look at the XML, and move on to step 6.

This style of processing is useful, but much of the functionality of Oscar3 is
best used through the server interface. It is easy to run Oscar3 to set up a
web server that is only accessible from your own computer.

6) Running the Oscar3 server. This is a little web server that lets you
access Oscar3 functionality via your web browser (Firefox is prefered). Don't
worry - by default the server is only accessible from the computer it is
running on.

To run the server: 

java -Xmx512m -jar oscar3-a5.jar Server

The first time you do this, you will be asked some questions. Answer these
(if in doubt, say "yes" to the yes/no questions, ask for a full server, and
just press return when asked where to put your workspace.

7) Connect your webbrowser (firefox is tested) to:

http://localhost:8181/

8) Explore the server, and have fun!

A lot of the functionality of the Oscar3 server revolves around the workspace
you generated on configuration. If you have not already created a workspace, and
do not wish to set the server up, then you can create a workspace by:

java -Xmx512m -jar oscar3-a5.jar ConfigureWorkspace

You may place papers in the workspace using the following method:

1) Make a directory

2) Put papers (or abstracts) in it, in HTML, plain text (filename must end in
".txt"), SciXML or for those who have the relevant stylesheet installed 
(if you don't know about it, you haven't got it installed) RSC XML. You can
make subdirectories and put papers in them, too, to any depth. NB: from here on,
when I say "papers" I mean "papers and/or abstracts".

3) Try:

java -Xmx512m -jar oscar3-a5.jar ImportAndProcess /absolute/path/to/directory

This will create a subdirectory of your "corpora" directory in the workspace -
this directory is a "corpus". You can look through the generated XML by hand,
or use the server functions below. This option also processs the files in the
directory. You can also use the command Import to fill a corpus with source
SciXML without parsing it, or MixedDirs to process a corpus. For "MixedDirs",
you can do this for a corpus in the right format anywhere in the filesystem -
alternatively, you can use a path relative to your "corpora" directory.

Alternatively, you can process a directory, and place the results in a new
directory somewhere else in your filesystem, without need for a workspace:

java -Xmx512m -jar oscar3-a5.jar ProcessInto /absolute/path/to/source/directory /absolute/path/to/destination/directory

One thing to try is the BioIE corpus. Download:

http://bioie.ldc.upenn.edu/publications/latest_release/data/CYP450/Entity/WordFreak_Annotation_Files/cyp_wf_ann_files_12-2.tgz

Extract the archive into a directory, and ImportAndProcess the directory.

Another way to get sets of papers to play with is to download them from PubMed:
there is an option on the main server page to do this. This queries PubMed with
the search term of your choice, downloads the number of abstracts you specify,
processs them and places the results in a directory in your workspace. Good
search terms to get started with include "enzyme" and "grapefruit"

Once you have some processed papers or abstracts in your workspace, there are
various things you can do with them. Starting at the main server page, click
on "View Papers", then click on links until you find yourself reading a paper.
With luck, some words should be highlighted in the paper, and some of those
should have underlines. Move the mouse over the underlined names to see the
chemical structure Oscar3 has assigned to the name. Try clicking on the names
to get an information page.

You can also use the Oscar3 server to do searches on the papers in your
workspace. From the main server page, click on "Select corpus for search", and
choose one of your sets of papers to index. Once these have been successfully 
indexed, go back to the main page, and try the "Search" and "CompoundsList"
options.

Peter Corbett, peter.corbett@cantab.net