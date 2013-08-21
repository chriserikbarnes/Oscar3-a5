<?xml version="1.0" encoding="UTF-8" ?>

<!--
	Document   : colourOutput.xsl
	Created on : 04 September 2004, 12:01
	Author     : caw47
	Description:
	Colour in marked-up paper for checking.
	
	Annexed by: ptc24, sometime late 2005. Grew lots of JavaScript
-->

<!--<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:cml="http://www.xml-cml.org/schema">
	<xsl:output method="html"
	doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
	doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"  
	media-type="application/xhtml+xml"/>-->

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:cml="http://www.xml-cml.org/schema">
	<xsl:output method="html" indent="yes" />
	<xsl:param name="jarpath"
		select="/processing-instruction('jarpath')" />
	<xsl:param name="host" select="/processing-instruction('host')" />
	<xsl:param name="viewer" select="/processing-instruction('viewer')" />
	<!-- <xsl:param name="viewer" select="''"/> -->
	<xsl:param name="printable"
		select="/processing-instruction('printable')" />

	<xsl:template name="citations">
		<xsl:param name="cits" select="''" />
		<xsl:choose>
			<xsl:when test="substring-before($cits, ' ')">
				<xsl:for-each
					select="/PAPER/REFERENCELIST/REFERENCE[@ID=substring-before($cits, ' ')] |
			    						/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=substring-before($cits, ' ')]">
					<xsl:apply-templates mode="for-attribute" />
				</xsl:for-each>
				<xsl:text>; </xsl:text>
				<xsl:call-template name="citations">
					<xsl:with-param name="cits"
						select="substring-after($cits, ' ')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each
					select="/PAPER/REFERENCELIST/REFERENCE[@ID=$cits]|
										/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$cits]">
					<xsl:apply-templates mode="for-attribute" />
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="strip-letters-from-front">
		<xsl:param name="val" select="''" />
		<xsl:choose>
			<xsl:when
				test="string-length(translate(substring(concat($val, '0'), 1, 1), 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', ''))=0">
				<xsl:call-template name="strip-letters-from-front">
					<xsl:with-param name="val"
						select="substring($val, 2)" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$val" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- ***** overall structure first ***** -->

	<xsl:template match="/">
		<html>
			<head>
				<!-- javascript for art -->
				<script type="text/javascript"
					src="/javascript/prototype-1.6.0.2.js" />
				<script type="text/javascript"
					src="/javascript/json.js" />	
				<script type="text/javascript"
					src="/javascript/art_tool.js" />

				<!--	<xsl:if test="/PAPER/METADATA/FILENO">-->
				<!--	  <meta>-->
				<!--	    <xsl:attribute name="name">fileno</xsl:attribute>-->
				<!--	    <xsl:attribute name="content"><xsl:value-of select="/PAPER/METADATA/FILENO"/></xsl:attribute>-->
				<!--	  </meta>-->
				<!--	</xsl:if>-->
				<style type="text/css">

					<!-- overall layout -->

					a.ne { color: #000000; text-decoration: none; }
					
					<!-- default for options in dropdowns -->
					option {background-color: white; color: black;} 
					
					<!-- default for topkey contents -->
					div.topkey { 
						display:inline; padding-left: 2px;
						padding-right: 2px; padding-top: 1px;
						padding-bottom:1px; font-weight: bold; color:white;
					} 
					
					
					div.sid { font-weight: bold; font-size: large; font-family: arial, sans-serif;}
					select.type option { color: white; }
					select.type option.None { color:black; }
					select.type option.default { color: #666; }
					
					<!-- Here put your preferred colours for your annotation concepts -->
					<!-- Put your own concepts here instead -->
					div.Apple, option.App 	{ background-color: #666; color: white;}
					div.Banana, option.Ban	{ background-color: #600; color: white;}
					div.Cherry, option.Che		{ background-color: #660; color: white;}
					div.Durian, option.Dur 	{ background-color: #060; color: white;} 
					div.Eggplant, option.Egg 		{ background-color: #066; color: white;} 
					div.Fortunella, option.For	{ background-color: #006; color: white;} 
					div.Grape, option.Gra 		{ background-color: #606; color: white;} 
					div.Huckleberry, option.Huc		{ background-color: #c00; color: white;} 
					div.Illawara, option.Ill  { background-color: #cc0; color: white;} 
					div.Jujube, option.Juj		{ background-color: #0c0; color: white;} 
					div.Kiwi, option.Kiw	{ background-color: #0cc; color: white;} 
					div.Lime, option.Lim		{ background-color: #00c; color: white;} 
					div.Melon, option.Mel		{ background-color: #c0c; color: white;}

					.sidebartitle { font-style: italic; }

					div.sidebar { right: 0px; width:
					29%; position: absolute; }
					p.sidebartitle{text-align: center;}
					div.sidebar2 { right: 0px; width: 29%; position:
					absolute; }
					input#toggle{margin-left:40%;}
textarea#comment{margin-bottom: 20%;}

					<!--					 <xsl:if test="$viewer">-->
					
					@media screen,print{
					div.mainpage { height: 99%; width: 70%; position:
					absolute; top: 2px; left: 0px; bottom: 2px; padding:
					2px; right: 320px; overflow: auto; }
					}
					<!--					</xsl:if>-->

					span.hide { display: none; }

					ul.referencelist { list-style-type: none }

					<!-- NE markup -->

					span.CM { background-color: #FFFF33; } span.ONT {
					background-color: #FFAA00; } span.ASE {
					background-color: #7700FF; } span.CJ {
					background-color: #AAAAFF; } span.RN {
					background-color: #00FF00; } span.CPR {
					background-color: #FF00FF; } span.DATA {
					background-color: #FF7777; } span.CUST {
					background-color: #CCCCCC; }

					<!-- Css for mode2 display of sentenceareas -->
					div.dropdowns{margin-left:6%;}				
					div.sid {
						text-align:right;
						float:left; 
						padding-right:1%; 
						min-width: 5%;
					}
					
					div.sentence{ 
						border-style: dotted;
						border-width: 1px; 
					}
					
					div.sentencearea{
						margin-bottom: 20px;
						display:block;
					}
					
					div.sentence{
						float:left;
						max-width: 90%;
						min-width: 85%;
					}
					
					div.toplinks { font-size: 90%; position:fixed; top: 0px; left: 0px; background-color:white;
					}
					
					div.allsentences { margin-top:10%;}


				</style>
				<title>
					<xsl:attribute name="filename">
						<xsl:value-of select="//mode2/@name" />
					</xsl:attribute>
					<xsl:value-of select="//mode2/@name" />
				</title>
			</head>
			<body onload="initialise()"> <!-- onload= "getOptions()"> -->
				
				<div class="mainpage" id="mainpage">
					<div class="toplinks">
					<p>
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=index</xsl:attribute>
							Index
						</xsl:element>
						|
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=showmode2&amp;sid=s1&amp;name=<xsl:value-of
													select="//mode2/@name" />
											
					</xsl:attribute>
							Refresh
						</xsl:element>
						|
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=autoannotate&amp;name=<xsl:value-of
													select="//mode2/@name"/>&amp;mode=2
					</xsl:attribute>
							Auto Annotate

						</xsl:element>
						|
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=clear&amp;name=<xsl:value-of
													select="//mode2/@name" />&amp;mode=2
					</xsl:attribute>
							Clear Auto Annotations
						</xsl:element>
						|
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=showMode2&amp;name=<xsl:value-of
													select="//mode2/@name" />&amp;mode=2
							</xsl:attribute>
							<xsl:attribute name="onclick">clearARTAnnotations();return false;</xsl:attribute>
							Clear Own Annotations
						</xsl:element>
						|
						
						<xsl:element name="a">
							<xsl:attribute name="href"></xsl:attribute> 
							<xsl:attribute name="onclick">savePaper();return false;</xsl:attribute>
							Save
						</xsl:element>
						|
						<xsl:element name="a">
							<xsl:attribute name="href">ART?action=help</xsl:attribute>
							Help
						</xsl:element>
					</p>
					<!-- Put your own key contents here, making sure each has the class "topkey"  -->
					<p>
						<div class="Apple topkey">Apple</div>
						<div class="Banana topkey">Banana</div>
						<div class="Cherry topkey">Cherry</div>
						<div class="Durian topkey">Durian</div>
						<div class="Eggplant topkey">Eggplant</div>
						<div class="Fortunella topkey">Fortunella</div>
						<div class="Grape topkey">Grape</div>
						<div class="Huckleberry topkey">Huckleberry</div>
						<div class="Illawara topkey">Illawara</div>
						<div class="Jujube topkey">Jujube</div>
						<div class="Kiwi topkey">Kiwi</div>
						<div class="Lime topkey">Lime</div>
						<div class="Melon topkey">Melon</div>
					</p>
					</div>
					<!-- SciXML papers -->
					<div class="allsentences">
						<xsl:for-each select="PAPER">
							<xsl:call-template name="PAPER" />
						</xsl:for-each>
					</div>
					<xsl:for-each select="attred">
						<xsl:apply-templates />
					</xsl:for-each>
				</div>
				<div class="sidebar">
					<p class="sidebartitle">Comments</p>
					<textarea id="comment" rows="20" cols="40" name="comment">
						<xsl:value-of select="@comment" />
					</textarea>
					<div id="oscarkey">
						
						<p class="sidebartitle">
							Oscar annotations key
						</p>
						<input id="toggle" type="button" value="Hide" onclick="toggleOscarKey('hide');"/>
						<ul id="oscarlist">
							<li>
								<span class="DATA">
									Experimental data
								</span>
							</li>
							<li>
								<span class="ONT">Ontology term</span>
							</li>
							<li>
								<span class="CM">
									<u>
										Chemical (etc.) with structure
									</u>
								</span>
							</li>
							<li>
								<span class="CM">
									Chemical (etc.), without structure
								</span>
							</li>
							<li>
								<span class="RN">Reaction</span>
							</li>
							<li>
								<span class="CJ">
									Chemical adjective
								</span>
							</li>
							<li>
								<span class="ASE">
									enzyme -ase word
								</span>
							</li>
							<li>
								<span class="CPR">Chemical prefix</span>
							</li>
						</ul>
					</div>
				</div>
				
				<xsl:if test="$viewer='applet'">
					<xsl:for-each
						select="/article/cmlPile|/PAPER/cmlPile">
						<xsl:apply-templates />
					</xsl:for-each>
				</xsl:if>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="TITLE">
		<title>
			<xsl:value-of select="." />
		</title>
	</xsl:template>

	<xsl:template match="PAPER" name="PAPER">
		<!--<xsl:for-each select="METADATA">
			<p>
				<xsl:apply-templates />
			</p>
		</xsl:for-each>-->
		<xsl:for-each select="TITLE|CURRENT_TITLE">
			<h2>
				<xsl:apply-templates />
			</h2>
		</xsl:for-each>
		<xsl:if test="not($printable)">
			<xsl:for-each select="scrapbook">
				<xsl:call-template name="scrapbook" />
			</xsl:for-each>
		</xsl:if>
		<!--<xsl:for-each select="CURRENT_AUTHORLIST">
			<ul>
				<xsl:apply-templates />
			</ul>
		</xsl:for-each>-->
		<xsl:for-each select="ABSTRACT">
			<hr />
			<p>
				<xsl:apply-templates />
			</p>
			<hr />
		</xsl:for-each>
		<xsl:for-each select="BODY/DIV">
			<xsl:apply-templates />
		</xsl:for-each>	
			<xsl:apply-templates select="s"/>
		
		<!--<xsl:for-each select="s">			
			<xsl:call-template name="s" />
		</xsl:for-each>-->
		<!--<xsl:for-each select="ACKNOWLEDGMENTS">
			<h2>Acknowledgements</h2>
			<p>
				<xsl:apply-templates />
			</p>
		</xsl:for-each>
		<xsl:for-each select="REFERENCELIST">
			<ul class="referencelist">
				<xsl:apply-templates />
			</ul>
		</xsl:for-each>
		<xsl:for-each select="FOOTNOTELIST">
			<xsl:apply-templates />
		</xsl:for-each>
		<xsl:for-each select="FIGURELIST">
			<xsl:apply-templates />
		</xsl:for-each>
		<xsl:for-each select="TABLELIST">
			<xsl:apply-templates />
		</xsl:for-each>-->
    <!--  </form>-->
	</xsl:template>

	<!-- ***** overall structure done ***** -->

	<!-- ***** SCIXML elements ***** -->

	<xsl:template match="P">
		<p>
			<xsl:if test="snippet">
				<xsl:attribute name="class">snippet</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates />
		</p>
	</xsl:template>
	
	<!-- change from toHTMLJS.xsl to allow sentences to be displayed -->
	<xsl:template name="s" match="s">
	   
		
		<!-- div that contains the sentence and its drop down options -->
		<div class="sentencearea">
			<xsl:attribute name="id"><xsl:value-of select="@sid"/>
			</xsl:attribute>
			<div class="sidsentence">
				<!--  div for displaying sentence id to the user -->
				<div class="sid">
					<xsl:value-of select="@sid"/>			
				</div>
			   <!-- div that contains the sentence text -->
				<div class="sentence">
				    <xsl:attribute name="sid"><xsl:value-of select="@sid"/>
					</xsl:attribute>
					<xsl:apply-templates />
				</div>
			</div>
			<div class="dropdowns">
				<xsl:choose>
				<!-- When a sentence contains annotations -->
				<!-- Put your own schema name here instead of "annotationART"-->
				<xsl:when test="annotationART">
					<select class="type" onchange="changeType(this,false)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
								Select a type
						</option>
						<!--  Put your own set of concepts in here in place of "None", "Bac" etc. -->
						<option class="None" value="None"><xsl:if test="annotationART[@type = 'None']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>None</option>
						<option class="App" value="App" ><xsl:if test="annotationART[@type = 'App']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Apple</option>
						<option class="Ban" value="Ban" ><xsl:if test="annotationART[@type = 'Ban']">								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Banana</option>
						<option class="Che" value="Che" ><xsl:if test="annotationART[@type = 'Che']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Cherry</option>
						<option class="Dur" value="Dur" ><xsl:if test="annotationART[@type = 'Dur']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Durian</option>
						<option class="Egg" value="Egg" ><xsl:if test="annotationART[@type = 'Egg']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Eggplant</option>
						<option class="For" value="For" ><xsl:if test="annotationART[@type = 'For']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Fortunella</option>
						<option class="Gra" value="Gra">
							<xsl:if test="annotationART[@type = 'Gra']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Grape</option>
						<option class="Huc" value="Huc" ><xsl:if test="annotationART[@type = 'Huc']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Model</option>
						<option class="Ill" value="Ill" ><xsl:if test="annotationART[@type = 'Ill']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Illawara</option>
						<option class="Juj" value="Juj" ><xsl:if test="annotationART[@type = 'Juj']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Jujube</option>
						<option class="Kiw" value="Kiw" ><xsl:if test="annotationART[@type = 'Kiw']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Kiwi</option>
						<option class="Lim" value="Lim" ><xsl:if test="annotationART[@type = 'Lim']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Lime</option>
						<option class="Mel" value="Mel" ><xsl:if test="annotationART[@type = 'Mel']">
								<xsl:attribute name="selected">selected</xsl:attribute>
							</xsl:if>Melon</option>	
					</select>
					<!-- if you are making use of subtypes, put your own subtype tests in the following "choose" block -->
					<select class="subtype" onchange="changeSubType(this)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
						</option>
						<xsl:variable name="sub">
							<xsl:choose>
								<xsl:when test="annotationART[@advantage = 'Yes']">Advantage</xsl:when>
								<xsl:when test="annotationART[@advantage = 'No']">Disadvantage</xsl:when>																	
								<xsl:when test="annotationART[@novelty = 'Old']">Old</xsl:when>
								<xsl:when test="annotationART[@novelty = 'New']">New</xsl:when>
								<xsl:otherwise>None</xsl:otherwise>
							</xsl:choose>
						</xsl:variable>
						<xsl:if test="$sub != 'None'">
							
							<option>
								<xsl:attribute name="class"><xsl:copy-of select="$sub"/></xsl:attribute>
								<xsl:attribute name="value"><xsl:copy-of select="$sub"/></xsl:attribute>
								<xsl:attribute name="selected">selected</xsl:attribute>
								<xsl:copy-of select="$sub"/>
							</option>
						</xsl:if>
					</select>
					<!-- If you have concepts which do not require concept IDs, leave this unchanged - otherwise put the name of your own schema -->
					<select class="conceptid" onchange="selectConceptID(this)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
						</option>
						<option>
							<xsl:attribute name="class"><xsl:value-of select="annotationART/@conceptID"/></xsl:attribute>
							<xsl:attribute name="value"><xsl:value-of select="annotationART/@conceptID"/></xsl:attribute>
							<xsl:attribute name="selected">selected</xsl:attribute>
							<xsl:value-of select="annotationART/@conceptID"/>
						</option>
					</select>
				</xsl:when>
				<!-- If a sentence has not yet been annotated -->
				<xsl:otherwise>				
					<select class="type" onchange="changeType(this,false)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
								Select a type
						</option>
						<!-- Put your own preferred options here -->
						<option class="None" value="None">None</option>
						<option class="App" value="App" >Apple</option>
						<option class="Ban" value="Ban" >Banana</option>
						<option class="Che" value="Che" >Cherry</option>
						<option class="Dur" value="Dur" >Durian</option>
						<option class="Egg" value="Egg" >Eggplant</option>
						<option class="For" value="For" >Fortunella</option>
						<option class="Gra" value="Gra" >Grape</option>
						<option class="Huc" value="Huc" >Huckleberry</option>
						<option class="Ill" value="Ill" >Illawara</option>
						<option class="Juj" value="Juj" >Jujube</option>
						<option class="Kiw" value="Kiw" >Kiwi</option>
						<option class="Lim" value="Lim" >Lime</option>
						<option class="Mel" value="Mel" >Melon</option>	
					</select>
					<!-- Leave this as it is - see javascript -->
					<select class="subtype" onchange="changeSubType(this)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
						</option>
					</select>
					<!-- Also leave this - see javascript -->
					<select class="conceptid" onchange="selectConceptID(this)">
						<xsl:attribute name="sid"><xsl:value-of select="@sid"/></xsl:attribute>
						<option class="default" value="default">
								<xsl:attribute name="selected">selected</xsl:attribute>
						</option>
					</select>
				</xsl:otherwise>
			</xsl:choose>
			</div> <!-- end of dropdowns -->
		</div>	<!-- End of sentence area -->	
	</xsl:template>

	<xsl:template match="SUBPAR">
		<br />
		<br />
	</xsl:template>

	<xsl:template match="DIV/HEADER" priority="-5">
		<h2>
			<xsl:apply-templates />
		</h2>
	</xsl:template>

	<xsl:template match="DIV/DIV/HEADER" priority="-4">
		<h3>
			<xsl:apply-templates />
		</h3>
	</xsl:template>

	<xsl:template match="DIV/DIV/DIV/HEADER" priority="-3">
		<h4>
			<xsl:apply-templates />
		</h4>
	</xsl:template>

	<xsl:template match="DIV/DIV/DIV/DIV/HEADER" priority="-2">
		<h5>
			<xsl:apply-templates />
		</h5>
	</xsl:template>

	<xsl:template match="DIV/DIV/DIV/DIV/DIV/HEADER" priority="-1">
		<h6>
			<xsl:apply-templates />
		</h6>
	</xsl:template>

	<xsl:template match="LIST[@TYPE='bullet']">
		<ul>
			<xsl:apply-templates />
		</ul>
	</xsl:template>

	<xsl:template match="LIST[@TYPE='number']">
		<ol>
			<xsl:apply-templates />
		</ol>
	</xsl:template>

	<xsl:template match="LI">
		<li>
			<xsl:apply-templates />
		</li>
	</xsl:template>

	<xsl:template match="DL">
		<dl>
			<xsl:apply-templates />
		</dl>
	</xsl:template>

	<xsl:template match="DT">
		<dt>
			<xsl:apply-templates />
		</dt>
	</xsl:template>

	<xsl:template match="DD">
		<dd>
			<xsl:apply-templates />
		</dd>
	</xsl:template>


	<xsl:template match="REF[@TYPE='P']|PUBREF">
		<xsl:choose>
			<xsl:when test="substring-before(@ID, ' ')">
				<a>
					<xsl:attribute name="href">#ref_<xsl:value-of
							select="substring-before(@ID, ' ')" />
	  </xsl:attribute>
					<xsl:attribute name="title">
	    <xsl:call-template name="citations">
	      <xsl:with-param name="cits" select="@ID" />
	    </xsl:call-template>
	  </xsl:attribute>
					<sup class="ref">
						<xsl:choose>
							<xsl:when test="string-length(.)=0">
								<xsl:call-template
									name="ref-string-to-ref-list">
									<xsl:with-param name="cits"
										select="@ID" />
								</xsl:call-template>
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates />
							</xsl:otherwise>
						</xsl:choose>
					</sup>
				</a>
			</xsl:when>
			<xsl:when test="@ID">
				<a href="#ref_{@ID}">
					<xsl:attribute name="title">
	    <xsl:call-template name="citations">
	      <xsl:with-param name="cits" select="@ID" />
	    </xsl:call-template>
	  </xsl:attribute>
					<sup class="ref">
						<xsl:choose>
							<xsl:when test="string-length(.)=0">
								<xsl:call-template
									name="ref-string-to-ref-list">
									<xsl:with-param name="cits"
										select="@ID" />
								</xsl:call-template>
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates />
							</xsl:otherwise>
						</xsl:choose>
					</sup>
				</a>
			</xsl:when>
			<xsl:otherwise>
				<sup class="ref">
					<xsl:apply-templates />
				</sup>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="ref-string-to-ref-list">
		<xsl:param name="cits" select="''" />
		<xsl:choose>
			<xsl:when test="substring-before($cits, ' ')">
				<xsl:call-template name="strip-letters-from-front">
					<xsl:with-param name="val"
						select="substring-before($cits, ' ')" />
				</xsl:call-template>
				,
				<xsl:call-template name="ref-string-to-ref-list">
					<xsl:with-param name="cits"
						select="substring-after($cits, ' ')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="strip-letters-from-front">
					<xsl:with-param name="val" select="$cits" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='COMPOUND']">
		<font style="color:#FF0000;">
			<b class="xrefc">
				<xsl:call-template name="structureLinks" />
			</b>
		</font>
	</xsl:template>

	<xsl:template
		match="XREF[@TYPE='FOOTNOTE_MARKER']|SUP[@TYPE='FOOTNOTE_MARKER']|XREF[@TYPE='FN-REF']">
		<a href="#footnote_{@ID}">
			<xsl:attribute name="title">
      <xsl:call-template name="footnote-for-id">
	<xsl:with-param name="id" select="@ID" />
      </xsl:call-template>
      </xsl:attribute>
			<sup class="fnref">
				<xsl:call-template name="footnote-marker">
					<xsl:with-param name="id" select="@ID" />
				</xsl:call-template>
			</sup>
		</a>
	</xsl:template>

	<xsl:template name="footnote-for-id">
		<xsl:param name="id" select="''" />
		<xsl:value-of select="/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$id]" />
	</xsl:template>

	<xsl:template name="footnote-marker">
		<xsl:param name="id" select="''" />
		<xsl:call-template name="footnote-marker-for-number">
			<xsl:with-param name="number"
				select="/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$id]/@MARKER" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="footnote-marker-for-number">
		<xsl:param name="number" select="''" />
		<xsl:choose>
			<xsl:when test="number($number)&lt;4">
				<xsl:value-of
					select="translate($number, '123', '*&#x2020;&#x2021;')" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="repeat-string">
					<xsl:with-param name="count"
						select="floor((number($number)-1) div 3)+1" />
					<!-- <xsl:with-param name="count" select="1"/> -->
					<xsl:with-param name="string"
						select="translate(string((number($number)-1) mod 3), '012', '*&#x2020;&#x2021;')" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="repeat-string">
		<xsl:param name="string" select="''" />
		<xsl:param name="count" select="''" />
		<xsl:if test="$count &gt; 0">
			<xsl:value-of select="$string" />
			<xsl:call-template name="repeat-string">
				<xsl:with-param name="string" select="$string" />
				<xsl:with-param name="count" select="number($count)-1" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- Also include GRAPH_POS when it exists -->

	<xsl:template match="XREF[@TYPE='EQN_POS']">
		<B>
			[EQUATION
			<xsl:value-of select="@ID" />
			]
		</B>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='THM-MARKER']">
		<b>
			<xsl:text />
			[THEOREM
			<xsl:value-of select="@ID" />
			]
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='TABLE-POS']">
		<b>
			<xsl:text />
			[TABLE
			<xsl:value-of select="@ID" />
			]
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='ILLUSTRATION-REF']">
		<b>
			<xsl:text />
			[ILLUSTRATION
			<xsl:value-of select="@ID" />
			]
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='IMG-REF']">
		<b>
			<xsl:text />
			[IMAGE
			<xsl:value-of select="@ID" />
			]
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template
		match="XREF[@TYPE='FIG-REF']|XREF[@TYPE='FIGURE-REF']|XREF[@TYPE='SCHEME-REF']|XREF[@TYPE='CHART-REF']">
		<a href="#fig_{@ID}">
			<xsl:call-template name="arrow-if-empty" />
		</a>
	</xsl:template>

	<xsl:template
		match="XREF[@TYPE='TABLE-REF']|XREF[@TYPE='BOX-REF']">
		<a href="#tab_{@ID}">
			<xsl:call-template name="arrow-if-empty" />
		</a>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='FD-REF']">
		<a href="#eqn_{@ID}">
			<xsl:apply-templates />
		</a>
	</xsl:template>

	<xsl:template match="XREF[@TYPE='PUBMED-REF']">
		<xsl:text />
		<a>
			<xsl:attribute name="href">http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=retrieve&amp;db=pubmed&amp;list_uids=<xsl:value-of
					select="@ID" />
			</xsl:attribute>
			->PUBMED
		</a>
		<xsl:text />
	</xsl:template>

	<xsl:template match="XREF[@TYPE='DOI-REF']">
		<xsl:text />
		<a>
			<xsl:attribute name="href">http://dx.doi.org/<xsl:value-of
					select="@ID" />
			</xsl:attribute>
			->DOI
		</a>
		<xsl:text />
	</xsl:template>

	<xsl:template match="XREF[@TYPE='IUCR-REF']">
		<xsl:text />
		<a>
			<xsl:attribute name="href">http://scripts.iucr.org/cgi-bin/paper?<xsl:value-of
					select="@ID" />
			</xsl:attribute>
			->IUCR
		</a>
		<xsl:text />
	</xsl:template>

	<xsl:template match="XREF" priority="-2">
		<B>
			<xsl:apply-templates />
		</B>
	</xsl:template>

	<xsl:template name="arrow-if-empty">
		<xsl:choose>
			<xsl:when test="string-length(.)&gt;0">
				<xsl:apply-templates />
			</xsl:when>
			<xsl:otherwise>-></xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="EQN">
		<xsl:if test="@ID">
			<a name="eqn_{@ID}" />
		</xsl:if>
		<blockquote>
			<xsl:apply-templates />
		</blockquote>
	</xsl:template>

	<xsl:template match="SB">
		<sub>
			<xsl:apply-templates />
		</sub>
	</xsl:template>

	<xsl:template match="SP">
		<sup>
			<xsl:apply-templates />
		</sup>
	</xsl:template>

	<xsl:template match="IT">
		<i>
			<xsl:apply-templates />
		</i>
	</xsl:template>

	<xsl:template match="B">
		<b>
			<xsl:apply-templates />
		</b>
	</xsl:template>

	<xsl:template match="UN">
		<u>
			<xsl:apply-templates />
		</u>
	</xsl:template>

	<xsl:template match="TYPE">
		<tt>
			<xsl:apply-templates />
		</tt>
	</xsl:template>

	<xsl:template match="LATEX">
		LATEX:{
		<xsl:apply-templates />
		}
	</xsl:template>

	<xsl:template match="SCP">
		<span style="font-variant: small-caps">
			<xsl:apply-templates />
		</span>
	</xsl:template>

	<xsl:template match="SANS">
		<span style="font-family: sans-serif">
			<xsl:apply-templates />
		</span>
	</xsl:template>

	<xsl:template match="ROMAN">
		<span style="font-style: normal">
			<xsl:apply-templates />
		</span>
	</xsl:template>

	<xsl:template match="URL">
		<xsl:element name="a">
			<xsl:attribute name="href"><xsl:value-of select="@HREF" />
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>

	<xsl:preserve-space elements="NAME" />

	<xsl:template match="REFERENCE" name="REFERENCE">
		<li>
			<!-- <xsl:if test="starts-with(@ID, 'cit')"> -->
			<!--<xsl:value-of select="substring(@ID, 4)"/> -->
			<!-- <xsl:value-of select="translate(@ID, 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', '')"/> -->
			<xsl:call-template name="strip-letters-from-front">
				<xsl:with-param name="val" select="@ID" />
			</xsl:call-template>
			<xsl:text>) </xsl:text>
			<!-- </xsl:if> -->
			<a name="ref_{@ID}" />
			<xsl:apply-templates />
		</li>
	</xsl:template>

	<xsl:template match="REFERENCE/AUTHORLIST/AUTHOR" priority="0">
		<xsl:apply-templates />
		<xsl:text>, </xsl:text>
	</xsl:template>

	<xsl:template match="AUTHOR|CURRENT_AUTHOR" priority="-1">
		<li>
			<xsl:apply-templates select="NAME|SURNAME" />
			<xsl:apply-templates select="PLACE" />
		</li>
	</xsl:template>

	<xsl:template match="NAME">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="SURNAME">
		<b>
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="SURNAME" mode="for-attribute">
		<xsl:text />
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="JOURNAL/NAME|BOOK/NAME">
		<i>
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</i>
	</xsl:template>

	<xsl:template match="JOURNAL/NAME|BOOK/NAME" mode="for-attribute">
		<xsl:text />
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="JOURNAL/YEAR|BOOK/YEAR">
		<b>
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="JOURNAL/YEAR|BOOK/YEAR" mode="for-attribute">
		<xsl:text />
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="JOURNAL/VOLUME|BOOK/VOLUME">
		<i>
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</i>
	</xsl:template>

	<xsl:template match="JOURNAL/VOLUME|BOOK/VOLUME"
		mode="for-attribute">
		<xsl:text />
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="JOURNAL/ISSUE|BOOK/ISSUE">
		<i>
			<xsl:text> (</xsl:text>
			<xsl:apply-templates />
			<xsl:text>) </xsl:text>
		</i>
	</xsl:template>

	<xsl:template match="JOURNAL/ISSUE|BOOK/ISSUE"
		mode="for-attribute">
		<xsl:text> (</xsl:text>
		<xsl:apply-templates />
		<xsl:text>) </xsl:text>
	</xsl:template>

	<xsl:template match="REFERENCE/TITLE">
		<xsl:if test="string-length(.)&gt;0">
			<xsl:text> "</xsl:text>
			<xsl:apply-templates />
			<xsl:text>" </xsl:text>
		</xsl:if>
	</xsl:template>

	<xsl:template match="REFERENCE/TITLE" mode="for-attribute">
		<xsl:if test="string-length(.)&gt;0">
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</xsl:if>
	</xsl:template>

	<xsl:template match="REFERENCE/DATE">
		<b>
			<xsl:text />
			<xsl:apply-templates />
			<xsl:text />
		</b>
	</xsl:template>

	<xsl:template match="REFERENCE/DATE" mode="for-attribute">
		<xsl:text />
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="FOOTNOTE">
		<p>
			<a name="footnote_{@ID}" />
			<xsl:choose>
				<xsl:when test="substring-after(@ID, 'cit')">
					<xsl:value-of select="substring-after(@ID, 'cit')" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template
						name="footnote-marker-for-number">
						<xsl:with-param name="number" select="@MARKER" />
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:text />
			<xsl:apply-templates />

		</p>
	</xsl:template>

	<xsl:template match="FILENO">
		<xsl:text>File Number: </xsl:text>
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="DOI">
		<xsl:text> DOI: </xsl:text>
		<a>
			<xsl:attribute name="href">http://dx.doi.org/<xsl:value-of
					select="." />
			</xsl:attribute>
			<xsl:apply-templates />
		</a>
		<xsl:text />
	</xsl:template>

	<xsl:template match="CLASSIFICATION">
		<b>
			<xsl:text> Keywords: </xsl:text>
		</b>
		<xsl:for-each select="KEYWORD">
			<xsl:apply-templates />
			<xsl:if test="following::*">
				<xsl:text>, </xsl:text>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="PAPERTYPE">
		<xsl:text> Type: </xsl:text>
		<xsl:apply-templates />
		<xsl:text />
	</xsl:template>

	<xsl:template match="FIGURE">
		<p>
			<a name="fig_{@ID}" />
			<b>
				<xsl:text>[</xsl:text>
				<xsl:value-of select="@ID" />
				<xsl:text />
				<xsl:value-of select="@SRC" />
				<xsl:text>] </xsl:text>
			</b>
			<xsl:apply-templates />
		</p>
	</xsl:template>

	<xsl:template match="FIGURE/TITLE">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="TABLE">
		<table border="1">
			<a name="tab_{@ID}" />
			<xsl:apply-templates />
			<xsl:if test="TITLE">
				<caption>
					<b>
						<xsl:text>TABLE </xsl:text>
						<xsl:call-template
							name="strip-letters-from-front">
							<xsl:with-param name="val" select="@ID" />
						</xsl:call-template>
						<xsl:text>: </xsl:text>
					</b>
					<xsl:for-each select="TITLE">
						<xsl:apply-templates />
					</xsl:for-each>
				</caption>
			</xsl:if>
		</table>
	</xsl:template>

	<xsl:template match="TABLE/TITLE" />

	<xsl:template match="TABLE/TGROUP/*/ROW">
		<tr>
			<xsl:apply-templates />
		</tr>
	</xsl:template>

	<xsl:template match="TABLE/TGROUP/THEAD/ROW/ENTRY">
		<th>
			<xsl:apply-templates />
		</th>
	</xsl:template>

	<xsl:template match="TABLE/TGROUP/TBODY/ROW/ENTRY">
		<td>
			<xsl:apply-templates />
		</td>
	</xsl:template>

	<!-- ***** SCIXML elements done ***** -->

	<!-- ***** OSCAR named-entity markup ***** -->

	<xsl:template match="ne" name="ne">
		<xsl:choose>
			<xsl:when test="not($printable)">
				<xsl:element name="span">
					<xsl:attribute name="class"><xsl:value-of
							select="@type" />
					</xsl:attribute>
					<xsl:attribute name="title"><xsl:for-each
							select="./@*[name()!='neid']"><xsl:value-of
								select="name()" /> = <xsl:value-of
								select="." />; </xsl:for-each>
					</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@SMILES">
							<u>
								<xsl:call-template
									name="structureLinks" />
							</u>
						</xsl:when>
						<xsl:when test="@Element">
							<u>
								<xsl:call-template
									name="structureLinks" />
							</u>
						</xsl:when>
						<xsl:otherwise>
							<xsl:call-template name="structureLinks" />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:element>
				<!--<xsl:if test="/PAPER/scrapbook[@mode='editor' or @mode='relEditor']">
					<xsl:element name="input">
					<xsl:attribute name="type">button</xsl:attribute>
					<xsl:attribute name="value">delete</xsl:attribute>
					<xsl:attribute name="onclick">
					w = window.open('ART?action=delne&amp;sid=<xsl:value-of select="ancestor::snippet/@id"/>&amp;neid=<xsl:value-of select="@neid"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>');  w.close(); document.location = document.location
					</xsl:attribute>
					</xsl:element>
					<xsl:element name="input">
					<xsl:attribute name="type">button</xsl:attribute>
					<xsl:attribute name="value">edit</xsl:attribute>
					<xsl:attribute name="onclick">
					document.location = 'ART?action=attred&amp;sid=<xsl:value-of select="ancestor::snippet/@id"/>&amp;neid=<xsl:value-of select="@neid"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'
					</xsl:attribute>
					</xsl:element>
					</xsl:if>-->
				<xsl:if test="@addButton">
					<form name="put" action="ChemNameDict"
						method="get">
						<input type="hidden" name="action" value="put" />
						<input name="name" type="hidden">
							<xsl:attribute name="value"><xsl:value-of
									select="." />
							</xsl:attribute>
						</input>
						<input name="smiles" type="hidden">
							<xsl:attribute name="value"><xsl:value-of
									select="@SMILES" />
							</xsl:attribute>
						</input>
						<input name="inchi" type="hidden">
							<xsl:attribute name="value"><xsl:value-of
									select="@InChI" />
							</xsl:attribute>
						</input>
						<input type="submit"
							value="Add to ChemNameDict" />
					</form>
				</xsl:if>
			</xsl:when>

			<xsl:otherwise>
				<u>
					<xsl:apply-templates />
				</u>
				<sub>
					<xsl:value-of select="@type" />
				</sub>
			</xsl:otherwise>

		</xsl:choose>
	</xsl:template>

	<xsl:template name="structureLinks">
		<xsl:choose>
			<xsl:when test="$viewer">
				<script type="text/javascript">
					datastore['chm
					<xsl:number level="any" count="*" />
					'] = {
					<xsl:if test="@cmlRef">
						"cmlRef": "
						<xsl:value-of select="@cmlRef" />
						",
					</xsl:if>
					<xsl:if test="@Element">
						"Element": "
						<xsl:value-of select="@Element" />
						",
					</xsl:if>
					<xsl:if test="@SMILES">
						"SMILES": "
						<xsl:value-of select="@SMILES" />
						",
					</xsl:if>
					<xsl:if test="@InChI">
						"InChI": "
						<xsl:value-of select="@InChI" />
						",
					</xsl:if>
					<xsl:if test="@ontIDs">
						"ontIDs": "
						<xsl:value-of select="@ontIDs" />
						",
					</xsl:if>
					<xsl:if test="@id">
						"id": "
						<xsl:value-of select="@id" />
						",
					</xsl:if>
					"Text": "
					<xsl:value-of select="." />
					", "Type": "
					<xsl:value-of select="@type" />
					" }
				</script>
				<xsl:element name="a">
					<xsl:attribute name="id">chm<xsl:number level="any"
							count="*" />
					</xsl:attribute>
					<xsl:attribute name="onmouseover">mouseon('chm<xsl:number
							level="any" count="*" />')</xsl:attribute>
					<xsl:attribute name="onmouseout">mouseoff('chm<xsl:number
							level="any" count="*" />')</xsl:attribute>
					<!-- This should be a simple href. However XSLT fails to contain a standard URIEncode function
						that's standard enough for firefox. Thus, we must use javascript. Oh well. -->
					<xsl:choose>
						<xsl:when test="$viewer='file' and @id">
							<xsl:attribute name="href"><xsl:value-of
									select="@id" />.html</xsl:attribute>
							<xsl:attribute name="class">ne</xsl:attribute>
						</xsl:when>
						<xsl:when test="not(/PAPER/scrapbook)">
							<xsl:attribute name="onclick">clickon('chm<xsl:number
									level="any" count="*" />')</xsl:attribute>
						</xsl:when>
					</xsl:choose>
					<xsl:apply-templates />
				</xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- This is utterly vile. But the world leaves me no choice: -->

	<xsl:template match="cmlUrlEnc">
		<xsl:element name="div">
			<xsl:attribute name="style">display: none</xsl:attribute>
			<xsl:attribute name="id"><xsl:value-of select="@idRef" />
			</xsl:attribute>
			<!-- <xsl:text disable-output-escaping="yes">&lt;!- -</xsl:text> -->
			<!--  <xsl:element name="cml" namespace="http://www.xml-cml.org/schema/">
				<xsl:for-each select="*">
				<xsl:copy-of select="." />
				</xsl:for-each>
				</xsl:element> -->
			<xsl:apply-templates />
			<!-- <xsl:text disable-output-escaping="yes">- -&gt;</xsl:text> -->
		</xsl:element>
	</xsl:template>

	<xsl:template match="cml:cml" />

	<!-- ***** OSCAR named-entity markup done ***** -->

	<!-- ***** OSCAR experimental section markup ***** -->

	<xsl:template match="datasection">
		<span style="font-family: monospace;">
			<xsl:apply-templates />
		</span>
	</xsl:template>

	<xsl:template match="property">
		<font style="background:#FFCCCC;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="spectrum[@type='ir']">
		<font style="background:#FF3333;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="spectrum[@type='uv']">
		<font style="background:#FF0000;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="spectrum[@type='hnmr']">
		<font style="background:#FF7777;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="spectrum[@type='cnmr']">
		<font style="background:#FFAAAA;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="spectrum[@type='massSpec']">
		<font style="background:#FFAACC;">
			<xsl:apply-templates />
		</font>
	</xsl:template>

	<xsl:template match="substance/property">
		<xsl:if test="@type='yield'">
			<font style="color:#FF8800;">
				<b>
					<xsl:apply-templates />
				</b>
			</font>
		</xsl:if>
		<xsl:if test="not(@type='yield')">
			<font style="color:#FF0088;">
				<b>
					<xsl:apply-templates />
				</b>
			</font>
		</xsl:if>
	</xsl:template>

	<!-- ***** OSCAR experimental section markup done ***** -->

	<!-- ***** Markup used by OSCAR web UI for general purposes ***** -->

	<xsl:template match="HEADER[@href]">
		<h3>
			<xsl:element name="a">
				<xsl:attribute name="href"><xsl:value-of select="@href" />
				</xsl:attribute>
				<xsl:apply-templates />
			</xsl:element>
		</h3>
	</xsl:template>

	<xsl:template match="a[@href]">
		<xsl:element name="a">
			<xsl:attribute name="href"><xsl:value-of select="@href" />
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>

	<!-- ***** Markup used by OSCAR web UI for general purposes done ***** -->

	<!-- ***** Markup used by ART ***** -->

	<xsl:template match="snippet">
		<xsl:choose>
			<xsl:when
				test="//scrapbook[@mode='selectorEditor' or @mode='booleanEditor' or @mode='textFieldEditor']">
				<xsl:apply-templates />
			</xsl:when>
			<xsl:when test="$printable">
				<xsl:apply-templates />
			</xsl:when>
			<xsl:otherwise>
				<form method="POST" action="ART">
					<!--	  <xsl:if test="/PAPER/scrapbook[@mode='editor']">-->
					<xsl:attribute name="ondblclick">addNeAtSel('compound')</xsl:attribute>
					<!--	  </xsl:if> -->
					<!--	  <xsl:if test="/PAPER/scrapbook[@mode='editor' or @mode='attreditor']">-->
					<xsl:attribute name="name"><xsl:value-of
							select="@id" />
					</xsl:attribute>
					<!--	  </xsl:if>-->
					<xsl:apply-templates />
					<!--<xsl:if
						test="/PAPER/scrapbook[@mode='show' or @mode='regtest']">
						<xsl:element name="input">
						<xsl:attribute name="type">button</xsl:attribute>
						<xsl:attribute name="value">Edit!</xsl:attribute>
						<xsl:attribute name="onclick">
						document.location = 'ART?action=showmode1&amp;sid=<xsl:value-of
						select="@id" />&amp;name=<xsl:value-of
						select="/PAPER/scrapbook/@name" />'
						</xsl:attribute>
						</xsl:element>
						</xsl:if>
					--><!-- <xsl:element name="input">
						<xsl:attribute name="type">button</xsl:attribute>
						<xsl:attribute name="value">Delete</xsl:attribute>
						<xsl:attribute name="onclick">
						w = window.open('ART?action=delete&amp;sid=<xsl:value-of select="@id"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'); w.close(); document.location = document.location
						</xsl:attribute>
						</xsl:element> -->
					<!-- <xsl:element name="input">
						<xsl:attribute name="type">button</xsl:attribute>
						<xsl:attribute name="value">Relations</xsl:attribute>
						<xsl:attribute name="onclick">
						document.location = 'ART?action=reledit&amp;sid=<xsl:value-of select="@id"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'
						</xsl:attribute>
						</xsl:element> -->
					<!-- <xsl:choose>
						<xsl:when test="/PAPER/scrapbook/@mode='relEditor'">
						<script>
						function addRel(x) {
						e = document.getElementById('relations')
						if(e.value.length > 0) e.value += ')\n'
						e.value += x + '('
						}
						
						function addRelItem(x) {
						e = document.getElementById('relations')
						if(e.value.substr(-1) != '(') e.value += ','
						e.value += x					
						}
						</script>
						<input type="hidden" name="action" value="submitrel"/>
						<input type="hidden" name="name">
						<xsl:attribute name="value"><xsl:value-of select="/PAPER/scrapbook/@name"/></xsl:attribute>
						</input>
						<input type="hidden" name="sid">
						<xsl:attribute name="value"><xsl:value-of select="@id"/></xsl:attribute>
						</input>
						<br/>
						<textarea rows="10" cols="80" name="relations" id="relations">
						<xsl:value-of select="@relations"/>
						</textarea>
						<br/>
						<input type="submit" value="Submit relations"/>	  	
						</xsl:when>
						<xsl:otherwise>
						<input type="hidden" name="action" value="comment"/>
						<input type="hidden" name="name">
						<xsl:attribute name="value"><xsl:value-of select="/PAPER/scrapbook/@name"/></xsl:attribute>
						</input>
						<input type="hidden" name="sid">
						<xsl:attribute name="value"><xsl:value-of select="@id"/></xsl:attribute>
						</input>
						<br/>
						<textarea rows="3" cols="80" name="comment">
						<xsl:value-of select="@comment"/>
						</textarea>
						<br/>
						<input type="submit" value="Submit comment"/>
						</xsl:otherwise>
						</xsl:choose>-->
				</form>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	
	<xsl:template name="scrapbook" match="scrapbook">
		<xsl:if test="not($printable)">

			<!-- scrapbook.xml hasDoc attribute
				<xsl:if test="not(@hasDoc)">
				|
				<xsl:element name="a">
				<xsl:attribute name="href">
				javascript:function getSelSource() { x = document.createElement('div'); x.appendChild(window.getSelection().getRangeAt(0).cloneContents()); return x.innerHTML; }
				src = getSelSource();
				wD = window.open().document;
				
				wD.write(&quot;&lt;html&gt;&lt;body&gt;&lt;h2&gt;ART Servlet&lt;/h2&gt;
				&lt;p&gt;Parsing - please wait...&lt;/p&gt;
				&lt;form id='thisForm' action='http://<xsl:value-of
				select="$host" />/ART' method='POST' accept-charset='UTF-8'&gt;
				&lt;input type='hidden' name='html' value='Foo Bar'&gt;
				&lt;input type='hidden' name='action' value='add'&gt;
				&lt;input type='hidden' name='fileno' value='unknown'&gt;
				&lt;input type='hidden' name='name' value='<xsl:value-of
				select="@name" />'&gt;
				&quot;);
				if(document.getElementsByName('fileno').length == 1) {
				wD.getElementById('thisForm').fileno.value = document.getElementsByName("fileno")[0].content;
				}
				wD.getElementById('thisForm').html.value = encodeURIComponent(src);
				wD.getElementById('thisForm').submit();		
				</xsl:attribute>
				-&gt;
				<xsl:value-of select="@name" />
				</xsl:element>
				</xsl:if>-->

			<p>
				<!-- To make edity things display when no edit mode, because this xsl is only used in what was edit mode anyway. -->
				<!--	<xsl:if test="self::node()[@mode='editor' or @mode='attreditor']">-->
				<script>
					<!--	    <xsl:if test="self::node()[@mode='editor']">-->
					function addNeAtSel(ne) {
					<!--	    </xsl:if>-->
					<!--	    <xsl:if test="self::node()[@mode='attreditor']">-->
					<!--	      function moveNeToSel(neid) {-->
					<!--	    </xsl:if>-->
					if(document.selection) { r =
					document.selection.createRange(); re = /ci="\d+"/g;
					nre = /\d+/; ht = r.htmlText; matches =
					ht.match(re); start =
					parseInt(matches[0].match(nre)) end =
					parseInt(matches[matches.length-1].match(nre)) + 1 n
					= r.parentElement(); } else { r =
					window.getSelection().getRangeAt(0); start =
					r.startContainer.parentNode.getAttribute("ci");
					start = parseInt(start); if(r.startOffset == 1)
					start += 1; end =
					r.endContainer.parentNode.getAttribute("ci"); end =
					parseInt(end); if(r.endOffset == 1) end += 1; n =
					r.startContainer; } while(n.tagName != 'FORM'
					&amp;&amp; n.tagName != 'HTML') { n = n.parentNode;
					} if(n.tagName == 'HTML' || !n.getAttribute('name'))
					{ alert("not in a form!"); } sid =
					n.getAttribute('name'); if(!sid) { alert("bad place
					for an annotation"); } else { cmd = "ART?sid=" + sid
					+ "&amp;start=" + start + "&amp;end=" + end;
					<!--		<xsl:if test="self::node()[@mode='editor']">-->
					cmd += "&amp;action=addne&amp;type=" + ne;
					<!--		</xsl:if>-->
					<!--		<xsl:if test="self::node()[@mode='attreditor']">-->
					<!--		  cmd += "&amp;action=movene&amp;neid=" + neid;-->
					<!--		</xsl:if>-->
					cmd += "&amp;name=
					<xsl:value-of select="/PAPER/scrapbook/@name" />
					"; w = window.open(cmd); w.close();
					document.location = document.location; } }
				</script>
				<xsl:apply-templates />
				<!--	</xsl:if>-->
			</p>
		</xsl:if>
	</xsl:template>
	<!--<xsl:template match="keydiv">
		<xsl:element name="div">
		<xsl:attribute name="class"><xsl:value-of select="@class" />
		</xsl:attribute>
		<xsl:value-of select="@class" />
		</xsl:element>
		</xsl:template>
	--><!-- <xsl:template match="nebutton">
		<xsl:element name="input">
		<xsl:attribute name="type">button</xsl:attribute>
		<xsl:attribute name="value"><xsl:value-of select="@type"/></xsl:attribute>
		<xsl:attribute name="onmousedown">
		addNeAtSel("<xsl:value-of select="@type"/>")
		</xsl:attribute>
		</xsl:element>
		</xsl:template> -->

	<xsl:template match="neAttrSel">
		<select>
			<xsl:attribute name="name"><xsl:value-of select="@name" />
			</xsl:attribute>
			<xsl:apply-templates />
		</select>
	</xsl:template>

	<xsl:template match="neAttrOpt">
		<option>
			<xsl:attribute name="value"><xsl:value-of select="@value" />
			</xsl:attribute>
			<xsl:if test="@selected">
				<xsl:attribute name="selected">selected</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="@value" />
		</option>
	</xsl:template>

	<xsl:template match="neTickyBox">
		<input type="checkbox">
			<xsl:attribute name="name"><xsl:value-of select="@name" />
			</xsl:attribute>
			<xsl:if test="@selected">
				<xsl:attribute name="checked">checked</xsl:attribute>
			</xsl:if>
		</input>
	</xsl:template>

	<xsl:template match="neTextEntry">
		<input type="text">
			<xsl:attribute name="name"><xsl:value-of select="@name" />
			</xsl:attribute>
			<xsl:if test="@value">
				<xsl:attribute name="value"><xsl:value-of
						select="@value" />
				</xsl:attribute>
			</xsl:if>
		</input>
	</xsl:template>

	<xsl:template match="neLabel">
		<!-- <b style="color: blue" onclick="document.getElementById('relations').value+='{@name},'"> -->
		<b style="color: blue" onclick="addRelItem('{@name}')">
			<xsl:text> [</xsl:text>
			<xsl:value-of select="@name" />
			<xsl:text>] </xsl:text>
		</b>
	</xsl:template>

	<xsl:template match="char">
		<xsl:element name="span">
			<xsl:attribute name="ci"><xsl:value-of select="@index" />
			</xsl:attribute>
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>

	<xsl:template match="attred">
		<h3>
			<xsl:for-each select="ne">
				<xsl:call-template name="ne" />
			</xsl:for-each>
			<xsl:element name="input">
				<xsl:attribute name="type">button</xsl:attribute>
				<xsl:attribute name="value">Move to selection</xsl:attribute>
				<xsl:attribute name="onmousedown">
	moveNeToSel("<xsl:value-of select="@neid" />")
      </xsl:attribute>
			</xsl:element>
		</h3>
		<xsl:element name="script">
			<xsl:attribute name="type">text/javascript</xsl:attribute>
			<xsl:text disable-output-escaping="yes">//&lt;![CDATA[
      <![CDATA[
	       function addRow(name) {
	         if(!name) return;
	         tablerow = document.createElement("tr");
	         document.getElementById("attrTable").appendChild(tablerow);
	         tablehead = document.createElement("th");
	         tablehead.innerHTML = name;
	         tablerow.appendChild(tablehead);
	         tabledata = document.createElement("td");
	         tabledata.innerHTML = "<input name='" + name + "' type='text'>"
	         tablerow.appendChild(tabledata);
	       }
      ]]>
      //]]&gt;
      </xsl:text>
		</xsl:element>
		<form action="ART" method="get" name="attrform">
			<input type="hidden" name="neid">
				<xsl:attribute name="value"><xsl:value-of
						select="@neid" />
				</xsl:attribute>
			</input>
			<input type="hidden" name="sid">
				<xsl:attribute name="value"><xsl:value-of select="@sid" />
				</xsl:attribute>
			</input>
			<input type="hidden" name="name">
				<xsl:attribute name="value"><xsl:value-of
						select="/PAPER/scrapbook/@name" />
				</xsl:attribute>
			</input>
			<input type="hidden" name="action" value="edattr" />
			<table>
				<tbody id="attrTable">
					<xsl:for-each select="attr">
						<xsl:call-template name="attr" />
					</xsl:for-each>
				</tbody>
			</table>
			<p>
				Add attribute:
				<input type="text" name="attrname" />
				<input type="button" value="Add!"
					onclick="addRow(document.forms['attrform'].attrname.value)" />
			</p>
			<input type="submit" value="Submit!" />
		</form>
	</xsl:template>

	<xsl:template name="attr">
		<tr>
			<th>
				<xsl:value-of select="@name" />
			</th>
			<td>
				<input type="text">
					<xsl:attribute name="name"><xsl:value-of
							select="@name" />
					</xsl:attribute>
					<xsl:attribute name="value"><xsl:value-of
							select="." />
					</xsl:attribute>
				</input>
			</td>
		</tr>
	</xsl:template>

	<!-- ***** Markup used by ART done ***** -->



</xsl:stylesheet>

