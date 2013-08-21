<?xml version="1.0" encoding="UTF-8" ?>

<!--
    Document   : colourOutput.xsl
    Created on : 04 September 2004, 12:01
    Author     : caw47
    Description:
        Colour in marked-up paper for checking.

    Annexed by: ptc24, sometime late 2005. Grew lots of JavaScript
-->

<!-- 
-->

<!--<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml"
		xmlns:cml="http://www.xml-cml.org/schema">
  <xsl:output method="html"
	      doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
	      doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
  
	      media-type="application/xhtml+xml"/>-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cml="http://www.xml-cml.org/schema">
  <xsl:output method="html" indent="yes"/>
  
  <xsl:param name="jarpath" select="/processing-instruction('jarpath')"/>
  <xsl:param name="host" select="/processing-instruction('host')"/>
  <xsl:param name="path" select="/processing-instruction('path')"/>
  <xsl:param name="viewer" select="/processing-instruction('viewer')"/>
  <xsl:param name="polymermode" select="/processing-instruction('polymermode')"/>
  <!-- <xsl:param name="viewer" select="''"/> -->
  <xsl:param name="printable" select="/processing-instruction('printable')"/>

  <xsl:template name="citations">
    <xsl:param name="cits" select="''" />
    <xsl:choose>
      <xsl:when test="substring-before($cits, ' ')">
	<xsl:for-each select="/PAPER/REFERENCELIST/REFERENCE[@ID=substring-before($cits, ' ')] |
			      /PAPER/FOOTNOTELIST/FOOTNOTE[@ID=substring-before($cits, ' ')]">
	  <xsl:apply-templates mode="for-attribute" />
	</xsl:for-each>
	<xsl:text>; </xsl:text>
	<xsl:call-template name="citations">
	  <xsl:with-param name="cits" select="substring-after($cits, ' ')"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:for-each select="/PAPER/REFERENCELIST/REFERENCE[@ID=$cits]|
			      /PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$cits]">
	  <xsl:apply-templates mode="for-attribute" />
	</xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="strip-letters-from-front">
    <xsl:param name="val" select="''"/>
    <xsl:choose>
      <xsl:when test="string-length(translate(substring(concat($val, '0'), 1, 1), 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', ''))=0">
	<xsl:call-template name="strip-letters-from-front">
	  <xsl:with-param name="val" select="substring($val, 2)"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$val"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ***** overall structure first ***** -->

  <xsl:template match="/">
    <html>
      <head>
	<title>
	  <xsl:for-each select="//titlegrp/title|/PAPER/TITLE">
	    <xsl:apply-templates mode="nodontdoit"/>
	  </xsl:for-each>
	</title>
	<xsl:if test="/PAPER/METADATA/FILENO">
	  <meta>
	    <xsl:attribute name="name">fileno</xsl:attribute>
	    <xsl:attribute name="content"><xsl:value-of select="/PAPER/METADATA/FILENO"/></xsl:attribute>
	  </meta>
	</xsl:if>
	<style type="text/css">

	  <!-- overall layout -->

	  a.ne {
		color: #000000;
		text-decoration: none;	  
	  }

	  div.sidebar {
	    text-align: center;
	    right: 0px;
	    width: 29%;
	    position: absolute;
	  }

	  div.sidebar2 {
	    right: 0px;
	    width: 29%;
	    position: absolute;
	  }

	  <xsl:if test="$viewer">
	  div.mainpage {
	    height: 99%;
	    width: 70%;
	    position: absolute;
	    top: 2px;
	    left: 0px;
	    bottom: 2px;
	    padding: 2px;
	    right: 320px;
	    overflow: auto;
	  }
	  </xsl:if>
	  
	  span.hide {
	    display: none;
	  }
	  
	  ul.referencelist {
	    list-style-type: none
	  }
	  
	  <!-- NE markup -->

	  span.CM {
	  	background-color: #FFFF33;
	  }
	  
	  span.CHEMICAL {
	  	background-color: #FFFF66;	    
	  }

      span.FORMULA {
        background-color: #AAFF66;
      }
      
      span.LIGAND {
        background-color: #FFAA66;
      }
      
      span.CLASS {
        background-color: #66AAFF;
      }

	  span.ONT {
	    background-color: #FFAA00;
	  }
	  
	  span.ASE {
	    background-color: #7700FF;
	  }

	  span.CJ {
	    background-color: #AAAAFF;
	  }
	  
	  span.RN {
	    background-color: #00FF00;
	  }

	  span.CPR {
	    background-color: #FF00FF;
	  }

	  span.DATA {
	  	background-color: #FF7777;
	  }
	  	  
	  span.PM {
	  	background-color: #F0E68C;
	  }
	  
	  span.CUST {
	  	background-color: #CCCCCC;
	  }

	  span.PRW {
	  	background-color: #CC99FF;
	  }
	  
	  span.protein {
	  	background-color: #00FFCC;
	  }
	  
	  span.DNA {
	  	background-color: #00FFCC;
	  }
	  
	  span.RNA {
	  	background-color: #00FFCC;	  
	  }
	  
	  span.cell_line {
	  	background-color: #00FFCC;	  
	  }
	  
	  span.cell_type {
	  	background-color: #00FFCC;	  
	  }
	  
	  <!-- Used by ScrapBook -->

	  p.snippet {
	  	border-style: dotted;
	  	border-width: 1px;
	  }
	  

	</style>
	<xsl:if test="$viewer">
	  <xsl:element name="script">
	    <xsl:attribute name="type">text/javascript</xsl:attribute>
	    var path = "<xsl:value-of select="$path"/>";
	    var viewerType = "<xsl:value-of select="$viewer"/>";
	    <xsl:text disable-output-escaping="yes">//&lt;![CDATA[
	    <![CDATA[
	  var lockmode = false;
	  var lockedElement = null;
	  var viewer = null;
	  function getViewer() {
	    if(document.applets[0]) {
	      document.applets[0].setLocation(location)
	      document.applets[0].setHost(document.location.host)
	      return document.applets[0]
	    } else if(document.viewer) {
	      document.viewer.setLocation(location)
	      return document.viewer
	    } else {
		  return 0;
	    }
	  }
	  
	  function setResults(HTML) {
	    getViewer().setOutputLabel(HTML)
	  }

	  function setSMILESImage(SMILES, tc) {
	    getViewer().setSMILES(SMILES, tc)
	  }

	  function setElementImage(sym, tc) {
	    getViewer().setElement(sym, tc)
	  }
	  
	  function setCMLImage(ref, tc) {
	    cml = document.getElementById(ref).childNodes[0].nodeValue;
	    getViewer().setEncodedCML(cml, tc);
	  }

	  function setSpanImage(spanContents, tc) {
	    if(spanContents.charAt(0) == "*") {
	      getViewer().setElement(spanContents.slice(1), tc)
	    } else {
    	  getViewer().setSMILES(spanContents, tc)
    	}
	  }

	  function setUnknown(tc) {
	    return getViewer().setUnknown(tc)
	  }

	  function clearImage() {
	    if(getViewer()) {
	    	getViewer().clear()
	    } else if(viewerType == "file") {
	    	document.getElementById("molImg").setAttribute("src", "blank.png");
	    } else {
	    	document.getElementById("molImg").setAttribute("src", path + "/ViewMol");
	    }
	  }
	  
	  function view(elemID) {
    	x = datastore[elemID];
	    if(getViewer()) {
		    if(x["cmlRef"] != null && document.getElementById(x["cmlRef"]) != null) {
		      setCMLImage(x["cmlRef"], x["text"]);
		      if(x["InChI"] != null) { getViewer().setInChI(x["InChI"], x["Text"]); }
		    } else if(x["SMILES"] != null) {
		      setSMILESImage(x["SMILES"], x["Text"]);
		      if(x["InChI"] != null) { getViewer().setInChI(x["InChI"], x["Text"]); }
		    } else if(x["Element"]) {
		      setElementImage(x["Element"], x["Text"]);
		    } else {
		      setUnknown(x["Text"]);
		    }
	    } else if(viewerType == "file") {
			if(x["id"] != null && x["InChI"] != null) {
				document.getElementById("molImg").setAttribute("src", x["id"] + ".png");
			} else {
				clearImage();
			}
	    } else {
		    if(x["InChI"] != null && x["SMILES"] != null) {
				document.getElementById("molImg").setAttribute("src", path + "/ViewMol?inchi=" + encodeURIComponent(x["InChI"]) + "&smiles=" + encodeURIComponent(x["SMILES"]));		    
			} else if(x["InChI"] != null || x["SMILES"] != null) {
				document.getElementById("molImg").setAttribute("src", path + "/ViewMol?inchi=" + encodeURIComponent(x["InChI"]));
			} else if(x["SMILES"] != null) {
				document.getElementById("molImg").setAttribute("src", path + "/ViewMol?smiles=" + encodeURIComponent(x["SMILES"]));
			} else if(x["Element"] != null) {
				document.getElementById("molImg").setAttribute("src", path + "/ViewMol?element=" + encodeURIComponent(x["Element"]));
		    } else {
				document.getElementById("molImg").setAttribute("src", path + "/ViewMol");
		    }
	    
	    }
	    
	    
	  }

	  function mouseon(elemID) {
	      view(elemID);
	  }
	  
	  function mouseoff(elem, elemID) {
	      clearImage();
	  }
	  
	  function clickon(elemID) {
	  	x = datastore[elemID];
	  	namepart = "name=" + encodeURIComponent(x["Text"]);
		typepart = "&type=" + encodeURIComponent(x["Type"]);
	    inchipart = ""
	    if(x["InChI"] != null) inchipart="&inchi=" + encodeURIComponent(x["InChI"]);
	    smilespart = ""
	    if(x["SMILES"] != null) smilespart="&smiles=" + encodeURIComponent(x["SMILES"]);
		ontpart = ""	  
	    if(x["ontIDs"] != null) ontpart="&ontids=" + encodeURIComponent(x["ontIDs"]);
		document.location = path + "/NEViewer?" + namepart + typepart + smilespart + inchipart + ontpart;
	  }
	  	  
	  ]]>
	  //]]&gt;
	    </xsl:text>
	  
	  var datastore = {};
	  
	  </xsl:element>
	</xsl:if>
      </head>
      <body>
    <xsl:choose>
	    <xsl:when test="/PAPER/scrapbook/relitems">
		<div class="sidebar2">
			<ul>
				<xsl:for-each select="/PAPER/scrapbook/relitems/rel">
					<li onclick="addRel('{.}')">
						<xsl:value-of select="."/>
					</li>
				</xsl:for-each>
			</ul>
		</div>    
	    </xsl:when>
	  <xsl:when test="$viewer">
	  <div class="sidebar">
	    <xsl:choose>
	      <xsl:when test="$viewer='picture'">
		<img id="molImg" src="/ViewMol"/>
	      </xsl:when>
	      <xsl:when test="$viewer='file'">
		<img id="molImg" src="blank.png"/>
	      </xsl:when>
	      <xsl:when test="$viewer='applet'">    
		<form name="appletForm">
		  <applet code="uk.ac.cam.ch.wwmm.viewerapplet.ViewerApplet"
			  width="300"
			  height="600"
			  name="viewer"
			  id="viewerApplet">
		    <xsl:attribute name="archive"><xsl:value-of select="$jarpath"/>viewer.jar,<xsl:value-of select="$jarpath"/>cdk-20050826.jar,<xsl:value-of select="$jarpath"/>ptclib.jar</xsl:attribute>
		    <param name="scriptable" value="true" />
		  </applet>
		</form>
	      </xsl:when>
	    </xsl:choose>
	    <ul>
	      <li><span class="DATA">Experimental data</span></li>
	      <li><span class="ONT">Ontology term</span></li>
	      <li><span class="CM"><u>Chemical (etc.) with structure</u></span></li>
	      <li><span class="CM">Chemical (etc.), without structure</span></li>
	      <xsl:if test="$polymermode"><li><span class="PM">Polymer</span></li></xsl:if>
	      <li><span class="RN">Reaction</span></li>
	      <li><span class="CJ">Chemical adjective</span></li>
	      <li><span class="ASE">enzyme -ase word</span></li>
	      <li><span class="CPR">Chemical prefix</span></li>
	    </ul> 
	</div>    
	  </xsl:when>
    </xsl:choose>
	<div class="mainpage">
	  
	  <!-- SciXML papers -->
	  <xsl:for-each select="PAPER">
	    <xsl:choose>
	      <xsl:when test="//scrapbook[@mode='selectorEditor' or @mode='booleanEditor' or @mode='textFieldEditor']">
		<form action="ScrapBook" method="POST">
		  <xsl:if test="//scrapbook[@mode='selectorEditor']">
		    <input type="hidden" name="action" value="selecteditsubmit"/>
		    <input type="hidden" name="type">
		    	<xsl:attribute name="value"><xsl:value-of select="//scrapbook/@selType"/></xsl:attribute>
		    </input>
		  </xsl:if>
		  <xsl:if test="//scrapbook[@mode='textFieldEditor']">
		    <input type="hidden" name="action" value="selecteditsubmit"/>
		    <input type="hidden" name="type">
		    	<xsl:attribute name="value"><xsl:value-of select="//scrapbook/@txtType"/></xsl:attribute>
		    </input>
		  </xsl:if>
		  
		  <xsl:if test="//scrapbook[@mode='booleanEditor']">
		    <input type="hidden" name="action" value="booleaneditsubmit"/>
		    <input type="hidden" name="attrName">
		      <xsl:attribute name="value"><xsl:value-of select="//scrapbook/@attrName"/></xsl:attribute>
		    </input>						
		  </xsl:if>			
		  <input type="hidden" name="name">
		    <xsl:attribute name="value"><xsl:value-of select="//scrapbook/@name"/></xsl:attribute>
		  </input>
		  <xsl:call-template name="PAPER"/>
		  <input type="submit" value="Submit!"/>			
		</form>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:call-template name="PAPER"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:for-each>
	  
	  <!-- -->
	  <xsl:for-each select="attred">
	    <xsl:apply-templates/>
	  </xsl:for-each>
	  
	</div>
	
	<xsl:if test="$viewer='applet'">
	  <xsl:for-each select="/article/cmlPile|/PAPER/cmlPile" >
	    <xsl:apply-templates/>
	  </xsl:for-each>
	</xsl:if>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="PAPER" name="PAPER">
    <xsl:for-each select="METADATA">
      <p><xsl:apply-templates/></p>
    </xsl:for-each>
    <xsl:for-each select="TITLE|CURRENT_TITLE">
      <h1><xsl:apply-templates/></h1>
    </xsl:for-each>
    <xsl:if test="not($printable)">
      <xsl:for-each select="scrapbook">
	<xsl:call-template name="scrapbook"/>
      </xsl:for-each>
    </xsl:if>
    <xsl:for-each select="CURRENT_AUTHORLIST">
      <ul><xsl:apply-templates/></ul>
    </xsl:for-each>
    <xsl:for-each select="ABSTRACT">
      <hr />
      <p><xsl:apply-templates/></p>
      <hr />
    </xsl:for-each>
    <xsl:for-each select="BODY/DIV">
      <xsl:apply-templates/>
    </xsl:for-each>
    <xsl:for-each select="ACKNOWLEDGMENTS">
      <h2>Acknowledgements</h2>
      <p><xsl:apply-templates/></p>
    </xsl:for-each>
    <xsl:for-each select="REFERENCELIST">
      <ul class="referencelist"><xsl:apply-templates/></ul>
    </xsl:for-each>
    <xsl:for-each select="FOOTNOTELIST">
      <xsl:apply-templates/>
    </xsl:for-each>
    <xsl:for-each select="FIGURELIST">
      <xsl:apply-templates/>
    </xsl:for-each>
    <xsl:for-each select="TABLELIST">
      <xsl:apply-templates/>
    </xsl:for-each>
    
  </xsl:template>
    
  <!-- ***** overall structure done ***** -->

  <!-- ***** SCIXML elements ***** -->

  <xsl:template match="P">
    <p><xsl:if test="snippet">
      <xsl:attribute name="class">snippet</xsl:attribute>
    </xsl:if>
    <xsl:apply-templates/></p>
  </xsl:template>
  
  <xsl:template match="SUBPAR">
    <br/><br/>
  </xsl:template>

  <xsl:template match="DIV/HEADER" priority="-5">
    <h2>
      <xsl:apply-templates/>
    </h2>
  </xsl:template>
  
  <xsl:template match="DIV/DIV/HEADER" priority="-4">
    <h3>
      <xsl:apply-templates/>
    </h3>
  </xsl:template>
  
  <xsl:template match="DIV/DIV/DIV/HEADER" priority="-3">
    <h4>
      <xsl:apply-templates/>
    </h4>
  </xsl:template>
  
  <xsl:template match="DIV/DIV/DIV/DIV/HEADER" priority="-2">
    <h5>
      <xsl:apply-templates/>
    </h5>
  </xsl:template>
  
  <xsl:template match="DIV/DIV/DIV/DIV/DIV/HEADER" priority="-1">
    <h6>
      <xsl:apply-templates/>
    </h6>
  </xsl:template>
  
  <xsl:template match="LIST[@TYPE='bullet']">
    <ul><xsl:apply-templates/></ul>
  </xsl:template>
  
  <xsl:template match="LIST[@TYPE='number']">
    <ol><xsl:apply-templates/></ol>
  </xsl:template>
  
  <xsl:template match="LI">
    <li><xsl:apply-templates/></li>
  </xsl:template>

  <xsl:template match="DL">
    <dl><xsl:apply-templates/></dl>
  </xsl:template>

  <xsl:template match="DT">
    <dt><xsl:apply-templates/></dt>
  </xsl:template>

  <xsl:template match="DD">
    <dd><xsl:apply-templates/></dd>
  </xsl:template>
  

  <xsl:template match="REF[@TYPE='P']|PUBREF">
    <xsl:choose>
      <xsl:when test="substring-before(@ID, ' ')">
	<a>
	  <xsl:attribute name="href">#ref_<xsl:value-of select="substring-before(@ID, ' ')"/>
	  </xsl:attribute>
	  <xsl:attribute name="title">
	    <xsl:call-template name="citations">
	      <xsl:with-param name="cits" select="@ID"/>
	    </xsl:call-template>
	  </xsl:attribute>
	  <sup class="ref">
	    <xsl:choose>
	      <xsl:when test="string-length(.)=0">
		<xsl:call-template name="ref-string-to-ref-list">
		  <xsl:with-param name="cits" select="@ID"/>
		</xsl:call-template>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:apply-templates/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </sup>
	</a>
      </xsl:when>
      <xsl:when test="@ID">
	<a href="#ref_{@ID}">
	  <xsl:attribute name="title">
	    <xsl:call-template name="citations">
	      <xsl:with-param name="cits" select="@ID"/>
	    </xsl:call-template>
	  </xsl:attribute>
	  <sup class="ref">
	    <xsl:choose>
	      <xsl:when test="string-length(.)=0">
		<xsl:call-template name="ref-string-to-ref-list">
		  <xsl:with-param name="cits" select="@ID"/>
		</xsl:call-template>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:apply-templates/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </sup>
	</a>
      </xsl:when>
      <xsl:otherwise>
	<sup class="ref">
	  <xsl:apply-templates/>
	</sup>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="ref-string-to-ref-list">
    <xsl:param name="cits" select="''"/>
    <xsl:choose>
      <xsl:when test="substring-before($cits, ' ')">
	<xsl:call-template name="strip-letters-from-front">
	  <xsl:with-param name="val" select="substring-before($cits, ' ')"/>
	</xsl:call-template>,<xsl:call-template name="ref-string-to-ref-list">
	  <xsl:with-param name="cits" select="substring-after($cits, ' ')"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="strip-letters-from-front">
	  <xsl:with-param name="val" select="$cits"/>
	</xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="XREF[@TYPE='COMPOUND']">
    <font style="color:#FF0000;"><b class="xrefc">
      <xsl:call-template name="structureLinks"/>
    </b></font>
  </xsl:template>
  
  <xsl:template match="XREF[@TYPE='FOOTNOTE_MARKER']|SUP[@TYPE='FOOTNOTE_MARKER']|XREF[@TYPE='FN-REF']">
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
    <xsl:param name="id" select="''"/>
    <xsl:value-of select="/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$id]" />
  </xsl:template>

  <xsl:template name="footnote-marker">
    <xsl:param name="id" select="''"/>
    <xsl:call-template name="footnote-marker-for-number">
      <xsl:with-param name="number" select="/PAPER/FOOTNOTELIST/FOOTNOTE[@ID=$id]/@MARKER"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="footnote-marker-for-number">
    <xsl:param name="number" select="''"/>
    <xsl:choose>
      <xsl:when test="number($number)&lt;4">
	<xsl:value-of select="translate($number, '123', '*&#x2020;&#x2021;')"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="repeat-string">
	  <xsl:with-param name="count" select="floor((number($number)-1) div 3)+1"/> 
	  <!-- <xsl:with-param name="count" select="1"/> -->
	  <xsl:with-param name="string" select="translate(string((number($number)-1) mod 3), '012', '*&#x2020;&#x2021;')"/>
	</xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="repeat-string">
    <xsl:param name="string" select="''"/>
    <xsl:param name="count" select="''"/>
    <xsl:if test="$count > 0">
      <xsl:value-of select="$string"/>
	<xsl:call-template name="repeat-string">
	  <xsl:with-param name="string" select="$string"/>
	  <xsl:with-param name="count" select="number($count)-1"/>
	</xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!-- Also include GRAPH_POS when it exists -->

  <xsl:template match="XREF[@TYPE='EQN_POS']">
    <B>[EQUATION <xsl:value-of select="@ID"/>]</B>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='THM-MARKER']">
    <b><xsl:text> </xsl:text>[THEOREM <xsl:value-of select="@ID"/>]<xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='TABLE-POS']">
    <b><xsl:text> </xsl:text>[TABLE <xsl:value-of select="@ID"/>]<xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='ILLUSTRATION-REF']">
    <b><xsl:text> </xsl:text>[ILLUSTRATION <xsl:value-of select="@ID"/>]<xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='IMG-REF']">
    <b><xsl:text> </xsl:text>[IMAGE <xsl:value-of select="@ID"/>]<xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='FIG-REF']|XREF[@TYPE='FIGURE-REF']|XREF[@TYPE='SCHEME-REF']|XREF[@TYPE='CHART-REF']">
    <a href="#fig_{@ID}"><xsl:call-template name="arrow-if-empty" /></a>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='TABLE-REF']|XREF[@TYPE='BOX-REF']">
    <a href="#tab_{@ID}"><xsl:call-template name="arrow-if-empty" /></a>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='FD-REF']">
    <a href="#eqn_{@ID}"><xsl:apply-templates /></a>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='PUBMED-REF']">
    <xsl:text> </xsl:text>
    <a>
      <xsl:attribute name="href">http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=retrieve&amp;db=pubmed&amp;list_uids=<xsl:value-of select="@ID"/></xsl:attribute>
      ->PUBMED
    </a>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='DOI-REF']">
    <xsl:text> </xsl:text>
    <a>
      <xsl:attribute name="href">http://dx.doi.org/<xsl:value-of select="@ID"/></xsl:attribute>
      ->DOI
    </a>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="XREF[@TYPE='IUCR-REF']">
    <xsl:text> </xsl:text>
    <a>
      <xsl:attribute name="href">http://scripts.iucr.org/cgi-bin/paper?<xsl:value-of select="@ID"/></xsl:attribute>
      ->IUCR
    </a>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="XREF" priority="-2">
    <B><xsl:apply-templates /></B>
  </xsl:template>

  <xsl:template name="arrow-if-empty">
    <xsl:choose>
      <xsl:when test="string-length(.)>0">
	<xsl:apply-templates />
      </xsl:when>
      <xsl:otherwise>
	->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="EQN">
    <xsl:if test="@ID">
      <a name="eqn_{@ID}"/>
    </xsl:if>
    <blockquote>
      <xsl:apply-templates />
    </blockquote>
  </xsl:template>

  <xsl:template match="SB">
    <sub>
      <xsl:apply-templates/>
    </sub>
  </xsl:template>
  
  <xsl:template match="SP"><sup><xsl:apply-templates/></sup></xsl:template>
  
  <xsl:template match="IT"><i><xsl:apply-templates/></i></xsl:template>
  
  <xsl:template match="B">
    <b>
      <xsl:apply-templates/>
    </b>
  </xsl:template>
  
  <xsl:template match="UN">
    <u>
      <xsl:apply-templates/>
    </u>
  </xsl:template>
  
  <xsl:template match="TYPE">
    <tt>
      <xsl:apply-templates/>
    </tt>
  </xsl:template>
  
  <xsl:template match="LATEX">
    LATEX:{<xsl:apply-templates/>}
  </xsl:template>
  
  <xsl:template match="SCP">
    <span style="font-variant: small-caps">
      <xsl:apply-templates/>
    </span>
  </xsl:template>
  
  <xsl:template match="SANS">
    <span style="font-family: sans-serif">
      <xsl:apply-templates/>
    </span>
  </xsl:template>
  
  <xsl:template match="ROMAN">
    <span style="font-style: normal">
      <xsl:apply-templates/>
    </span>
  </xsl:template>
  
<xsl:template match="URL">
    <xsl:element name="a">
      <xsl:attribute name="href"><xsl:value-of select="@HREF"/></xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:preserve-space elements="NAME" />
  
  <xsl:template match="REFERENCE" name="REFERENCE">
    <li>
      <!-- <xsl:if test="starts-with(@ID, 'cit')"> -->
	<!--<xsl:value-of select="substring(@ID, 4)"/> -->
	<!-- <xsl:value-of select="translate(@ID, 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', '')"/> -->
	<xsl:call-template name="strip-letters-from-front">
	  <xsl:with-param name="val" select="@ID"/>
	</xsl:call-template>
	<xsl:text>) </xsl:text>
      <!-- </xsl:if> -->
      <a name="ref_{@ID}" />
      <xsl:apply-templates />
    </li>
  </xsl:template>
  
  <xsl:template match="REFERENCE/AUTHORLIST/AUTHOR" priority="0">
    <xsl:apply-templates /><xsl:text>, </xsl:text>
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
    <b><xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="SURNAME" mode="for-attribute">
    <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>
  
  <xsl:template match="JOURNAL/NAME|BOOK/NAME">
    <i><xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text></i>
  </xsl:template>

  <xsl:template match="JOURNAL/NAME|BOOK/NAME" mode="for-attribute">
    <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>
  
  <xsl:template match="JOURNAL/YEAR|BOOK/YEAR">
    <b><xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="JOURNAL/YEAR|BOOK/YEAR" mode="for-attribute">
    <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="JOURNAL/VOLUME|BOOK/VOLUME">
    <i><xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text></i>
  </xsl:template>

  <xsl:template match="JOURNAL/VOLUME|BOOK/VOLUME" mode="for-attribute">
    <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="JOURNAL/ISSUE|BOOK/ISSUE">
    <i><xsl:text> (</xsl:text><xsl:apply-templates /><xsl:text>) </xsl:text></i>
  </xsl:template>

  <xsl:template match="JOURNAL/ISSUE|BOOK/ISSUE" mode="for-attribute">
    <xsl:text> (</xsl:text><xsl:apply-templates /><xsl:text>) </xsl:text>
  </xsl:template>

  <xsl:template match="REFERENCE/TITLE">
    <xsl:if test="string-length(.)>0">
      <xsl:text> "</xsl:text><xsl:apply-templates /><xsl:text>" </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="REFERENCE/TITLE" mode="for-attribute">
    <xsl:if test="string-length(.)>0">
      <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="REFERENCE/DATE">
    <b><xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text></b>
  </xsl:template>

  <xsl:template match="REFERENCE/DATE" mode="for-attribute">
    <xsl:text> </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="FOOTNOTE">
    <p>
      <a name="footnote_{@ID}" />
      <xsl:choose>
	<xsl:when test="substring-after(@ID, 'cit')">
	  <xsl:value-of select="substring-after(@ID, 'cit')"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:call-template name="footnote-marker-for-number">
	    <xsl:with-param name="number" select="@MARKER"/>
	  </xsl:call-template>
	</xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
      <xsl:apply-templates />

    </p>
  </xsl:template>

  <xsl:template match="FILENO">
    <xsl:text>File Number: </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="DOI">
    <xsl:text> DOI: </xsl:text>    
    <a>
      <xsl:attribute name="href">http://dx.doi.org/<xsl:value-of select="."/></xsl:attribute>
    <xsl:apply-templates />
    </a>
    <xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="CLASSIFICATION">
    <b><xsl:text> Keywords: </xsl:text></b>
    <xsl:for-each select="KEYWORD">
      <xsl:apply-templates/>
      <xsl:if test="following::*">
	<xsl:text>, </xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="PAPERTYPE">
    <xsl:text> Type: </xsl:text><xsl:apply-templates /><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="FIGURE">
    <p>
      <a name="fig_{@ID}"/>
      <b>
	<xsl:text>[</xsl:text>
	<xsl:value-of select="@ID"/>
	<xsl:text> </xsl:text>
	<xsl:value-of select="@SRC"/>
      <xsl:text>] </xsl:text></b>
      <xsl:apply-templates />
    </p>
  </xsl:template>

  <xsl:template match="FIGURE/TITLE">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="TABLE">
    <table border="1">
      <a name="tab_{@ID}"/>
      <xsl:apply-templates />
      <xsl:if test="TITLE">
	<caption>
	  <b>
	    <xsl:text>TABLE </xsl:text>
	    <xsl:call-template name="strip-letters-from-front">
	      <xsl:with-param name="val" select="@ID"/>
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
    <tr><xsl:apply-templates /></tr>
  </xsl:template>

  <xsl:template match="TABLE/TGROUP/THEAD/ROW/ENTRY">
    <th><xsl:apply-templates /></th>
  </xsl:template>

  <xsl:template match="TABLE/TGROUP/TBODY/ROW/ENTRY">
    <td><xsl:apply-templates /></td>
  </xsl:template>

  <!-- ***** SCIXML elements done ***** -->

  <!-- ***** OSCAR named-entity markup ***** -->

	<xsl:template match="ne" name="ne">
    	<xsl:choose>    
	   <xsl:when test="not($printable)">  
		<xsl:element name="span">
	  <xsl:attribute name="class"><xsl:value-of select="@type"/></xsl:attribute>
	  <xsl:attribute name="title"><xsl:for-each select="./@*[name()!='neid']"><xsl:value-of select="name()"/> = <xsl:value-of select="."/>; </xsl:for-each></xsl:attribute>
	  <xsl:choose>
	    <xsl:when test="@InChI">
	      <u><xsl:call-template name="structureLinks"/></u>
	    </xsl:when>
	    <xsl:when test="@Element">
	      <u><xsl:call-template name="structureLinks"/></u>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:call-template name="structureLinks"/>	
	    </xsl:otherwise>
	  </xsl:choose>  	   
	</xsl:element>
	<xsl:if test="/PAPER/scrapbook[@mode='editor' or @mode='relEditor']">
	  <xsl:element name="input">
	    <xsl:attribute name="type">button</xsl:attribute>
	    <xsl:attribute name="value">delete</xsl:attribute>
	    <xsl:attribute name="onclick">
	      w = window.open('ScrapBook?action=delne&amp;sid=<xsl:value-of select="ancestor::snippet/@id"/>&amp;neid=<xsl:value-of select="@neid"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>');  w.close(); document.location = document.location
	    </xsl:attribute>
	  </xsl:element>
	  <xsl:element name="input">
	    <xsl:attribute name="type">button</xsl:attribute>
	    <xsl:attribute name="value">edit</xsl:attribute>
	    <xsl:attribute name="onclick">
	      document.location = 'ScrapBook?action=attred&amp;sid=<xsl:value-of select="ancestor::snippet/@id"/>&amp;neid=<xsl:value-of select="@neid"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'
	    </xsl:attribute>
	  </xsl:element>
	</xsl:if>
	<xsl:if test="@addButton">
	  <form name="put" action="ChemNameDict" method="get">
	    <input type="hidden" name="action" value="put"></input>
	    <input name="name" type="hidden">
	      <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
	    </input>
	    <input name="smiles" type="hidden">
	      <xsl:attribute name="value"><xsl:value-of select="@SMILES"/></xsl:attribute>			
	    </input>
	    <input name="inchi" type="hidden">
	      <xsl:attribute name="value"><xsl:value-of select="@InChI"/></xsl:attribute>			
	    </input>
	    <input type="submit" value="Add to ChemNameDict"></input>
	  </form>
	</xsl:if>
      </xsl:when>
      
      <xsl:otherwise>
	<u><xsl:apply-templates/></u>
	<sub><xsl:value-of select="@type"/></sub>
      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>
    
  <xsl:template name="structureLinks">
    <xsl:choose>
      <xsl:when test="$viewer">
	<script type="text/javascript">
	  datastore['chm<xsl:number level="any" count="*"/>'] = {
	  <xsl:if test="@cmlRef">"cmlRef": "<xsl:value-of select="@cmlRef"/>", </xsl:if>
	  <xsl:if test="@Element">"Element": "<xsl:value-of select="@Element"/>", </xsl:if>
	  <xsl:if test="@SMILES">"SMILES": "<xsl:value-of select="@SMILES"/>", </xsl:if>
	  <xsl:if test="@InChI">"InChI": "<xsl:value-of select="@InChI"/>", </xsl:if>
	  <xsl:if test="@ontIDs">"ontIDs": "<xsl:value-of select="@ontIDs"/>", </xsl:if>
	  <xsl:if test="@id">"id": "<xsl:value-of select="@id"/>", </xsl:if>
	  "Text": "<xsl:value-of select="."/>",
	  "Type": "<xsl:value-of select="@type"/>"
	  }
	</script>
	<xsl:element name="a">
	  <xsl:attribute name="id">chm<xsl:number level="any" count="*"/></xsl:attribute>
	  <xsl:attribute name="onmouseover">mouseon('chm<xsl:number level="any" count="*"/>')</xsl:attribute>
	  <xsl:attribute name="onmouseout">mouseoff('chm<xsl:number level="any" count="*"/>')</xsl:attribute>
	  <!-- This should be a simple href. However XSLT fails to contain a standard URIEncode function
	       that's standard enough for firefox. Thus, we must use javascript. Oh well. -->
	  <xsl:choose>
	  <xsl:when test="$viewer='file' and @id">
	  	<xsl:attribute name="href"><xsl:value-of select="@id"/>.html</xsl:attribute>
	  	<xsl:attribute name="class">ne</xsl:attribute>
	  </xsl:when>
	  <xsl:when test="not(/PAPER/scrapbook)">
		<xsl:attribute name="onclick">clickon('chm<xsl:number level="any" count="*"/>')</xsl:attribute>
	  </xsl:when>
	  </xsl:choose>
	  <xsl:apply-templates/>
	</xsl:element>
      </xsl:when>
      <xsl:otherwise><xsl:apply-templates/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- This is utterly vile. But the world leaves me no choice: -->

  <xsl:template match="cmlUrlEnc">
    <xsl:element name="div">
      <xsl:attribute name="style">display: none</xsl:attribute>
      <xsl:attribute name="id"><xsl:value-of select="@idRef"/></xsl:attribute>
      <!-- <xsl:text disable-output-escaping="yes">&lt;!- -</xsl:text> -->
      <!--  <xsl:element name="cml" namespace="http://www.xml-cml.org/schema/">
	   <xsl:for-each select="*">
	   <xsl:copy-of select="." />
	   </xsl:for-each>
	   </xsl:element> -->
      <xsl:apply-templates/>
      <!-- <xsl:text disable-output-escaping="yes">- -&gt;</xsl:text> -->
    </xsl:element>
  </xsl:template>
  
  <xsl:template match="cml:cml"/>

  <!-- ***** OSCAR named-entity markup done ***** -->

  <!-- ***** OSCAR experimental section markup ***** -->

  <xsl:template match="datasection">
    <span style="font-family: monospace;">
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match="property">
    <font style="background:#FFCCCC;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  
  <xsl:template match="spectrum[@type='ir']">
    <font style="background:#FF3333;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  
  <xsl:template match="spectrum[@type='uv']">
    <font style="background:#FF0000;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  
  <xsl:template match="spectrum[@type='hnmr']">
    <font style="background:#FF7777;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  
  <xsl:template match="spectrum[@type='cnmr']">
    <font style="background:#FFAAAA;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
  
  <xsl:template match="spectrum[@type='massSpec']">
    <font style="background:#FFAACC;">
      <xsl:apply-templates/>
    </font>
  </xsl:template>
    
  <xsl:template match="substance/property">
    <xsl:if test="@type='yield'">
      <font style="color:#FF8800;"><b>
	<xsl:apply-templates/>
      </b></font>
    </xsl:if>
    <xsl:if test="not(@type='yield')">
      <font style="color:#FF0088;"><b>
	<xsl:apply-templates/>
      </b></font>
    </xsl:if>
  </xsl:template>

  <!-- ***** OSCAR experimental section markup done ***** -->

  <!-- ***** Markup used by OSCAR web UI for general purposes ***** -->

  <xsl:template match="HEADER[@href]">
    <h3>
      <xsl:element name="a">
	<xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
	<xsl:apply-templates/>
      </xsl:element>
    </h3>
  </xsl:template>
  
  <xsl:template match="a[@href]">
    <xsl:element name="a">
      <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <!-- ***** Markup used by OSCAR web UI for general purposes done ***** -->

  <!-- ***** Markup used by ScrapBook ***** -->

  <xsl:template match="snippet">
    <xsl:choose>
      <xsl:when test="//scrapbook[@mode='selectorEditor' or @mode='booleanEditor' or @mode='textFieldEditor']">
	<xsl:apply-templates/>	
      </xsl:when>
      <xsl:when test="$printable">
	<xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
	<form method="POST" action="ScrapBook">
	  <!-- <xsl:if test="/PAPER/scrapbook[@mode='editor']">
	    <xsl:attribute name="ondblclick">addNeAtSel('compound')</xsl:attribute>
	  </xsl:if> -->
	  <xsl:if test="/PAPER/scrapbook[@mode='editor' or @mode='attreditor']">
	    <xsl:attribute name="name"><xsl:value-of select="@id"/></xsl:attribute>
	  </xsl:if>
	  <xsl:apply-templates/>
	  <xsl:if test="/PAPER/scrapbook[@mode='show' or @mode='regtest']">
	    <xsl:element name="input">
	      <xsl:attribute name="type">button</xsl:attribute>
	      <xsl:attribute name="value">Edit!</xsl:attribute>
	      <xsl:attribute name="onclick">
		document.location = 'ScrapBook?action=edit&amp;sid=<xsl:value-of select="@id"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'
	      </xsl:attribute>
	    </xsl:element>
	  </xsl:if>
	  <xsl:element name="input">
	    <xsl:attribute name="type">button</xsl:attribute>
	    <xsl:attribute name="value">Delete</xsl:attribute>
	    <xsl:attribute name="onclick">
	      w = window.open('ScrapBook?action=delete&amp;sid=<xsl:value-of select="@id"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'); w.close(); document.location = document.location
	    </xsl:attribute>
	  </xsl:element>
	  <!-- <xsl:element name="input">
	    <xsl:attribute name="type">button</xsl:attribute>
	    <xsl:attribute name="value">Relations</xsl:attribute>
	    <xsl:attribute name="onclick">
		document.location = 'ScrapBook?action=reledit&amp;sid=<xsl:value-of select="@id"/>&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>'
	    </xsl:attribute>
	  </xsl:element> -->
	  <xsl:choose>
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
	  </xsl:choose>
	</form>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="scrapbook" match="scrapbook">
    <xsl:if test="not($printable)">
      <p>
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=show&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Show
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=selectedit&amp;type=type&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Edit Types
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=selectedit&amp;type=subtype&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Edit Subtypes
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=selectedit&amp;type=ont&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Edit Ontology IDs
	</xsl:element>
	| 
	<!-- <xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=textfieldedit&amp;type=relation&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Edit Relations
	</xsl:element>
	|  -->
	<!-- <xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=renderrel&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Render Relations
	</xsl:element>
	| -->
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=requestbooleanedit&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Boolean Attributes
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=autoannotate&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Auto Annotate
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=autoannotatereactions&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Auto Annotate (Reactions)
	</xsl:element>
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=clear&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Clear Annotations
	</xsl:element> 
	| 
	<xsl:element name="a">
	  <xsl:attribute name="href">ScrapBook?action=regtest&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	  Regression Test
	</xsl:element>
	<xsl:if test="@hasDoc">
	  | 
	  <xsl:element name="a">
	    <xsl:attribute name="href">ScrapBook?action=makepaper&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	    SciXML Paper
	  </xsl:element>	
	</xsl:if>
	<xsl:if test="@hasPubXMLDoc">
	  | 
	  <xsl:element name="a">
	    <xsl:attribute name="href">ScrapBook?action=makepubxmlpaper&amp;name=<xsl:value-of select="@name"/></xsl:attribute>
	    Publisher's XML Paper
	  </xsl:element>
	</xsl:if>
	|
	<xsl:element name="a"> 
	  <xsl:attribute name="href">ScrapBook?action=index</xsl:attribute>
	  Index
	</xsl:element>
	|
	<xsl:element name="a"> 
	  <xsl:attribute name="href">/docs/scrapbook.html</xsl:attribute>
	  Help
	</xsl:element> 
	<xsl:if test="not(@hasDoc)">
	  | 
	  <xsl:element name="a">
	    <xsl:attribute name="href">
	      javascript:function getSelSource() { x = document.createElement('div'); x.appendChild(window.getSelection().getRangeAt(0).cloneContents()); return x.innerHTML; }
	      src = getSelSource();
	      wD = window.open().document;
	      
	      wD.write(&quot;&lt;html&gt;&lt;body&gt;&lt;h2&gt;ScrapBook Servlet&lt;/h2&gt;
	      &lt;p&gt;Parsing - please wait...&lt;/p&gt;
	      &lt;form id='thisForm' action='http://<xsl:value-of select="$host"/><xsl:value-of select="$path"/>/ScrapBook' method='POST' accept-charset='UTF-8'&gt;
	      &lt;input type='hidden' name='html' value='Foo Bar'&gt;
	      &lt;input type='hidden' name='action' value='add'&gt;
	      &lt;input type='hidden' name='fileno' value='unknown'&gt;
	      &lt;input type='hidden' name='name' value='<xsl:value-of select="@name"/>'&gt;
	      &quot;);
	      if(document.getElementsByName('fileno').length == 1) {
	        wD.getElementById('thisForm').fileno.value = document.getElementsByName("fileno")[0].content;
	      }
	      wD.getElementById('thisForm').html.value = encodeURIComponent(src);
	      wD.getElementById('thisForm').submit();		
	    </xsl:attribute>
	    -&gt; 
	    <xsl:value-of select="@name"/>
	  </xsl:element>
	</xsl:if>
      </p>
      <p>
	<xsl:if test="self::node()[@mode='editor' or @mode='attreditor']">
	  <script>
	    <xsl:if test="self::node()[@mode='editor']">
	      function addNeAtSel(ne) {
	    </xsl:if>
	    <xsl:if test="self::node()[@mode='attreditor']">
	      function moveNeToSel(neid) {
	    </xsl:if>
	      if(document.selection) {
		  r = document.selection.createRange();
		  re = /ci="\d+"/g;
		  nre = /\d+/;
		  ht = r.htmlText;
		  matches = ht.match(re);
		  start = parseInt(matches[0].match(nre))
		  end = parseInt(matches[matches.length-1].match(nre)) + 1
		  n = r.parentElement();
		} else {		 
		  r = window.getSelection().getRangeAt(0);
		  start = r.startContainer.parentNode.getAttribute("ci");
		  start = parseInt(start);
		  if(r.startOffset == 1) start += 1;
		  end = r.endContainer.parentNode.getAttribute("ci");
		  end = parseInt(end);
		  if(r.endOffset == 1) end += 1;
		  n = r.startContainer;
		}
		while(n.tagName != 'FORM' &amp;&amp; n.tagName != 'HTML') {
		  n = n.parentNode;
		}
		if(n.tagName == 'HTML' || !n.getAttribute('name')) {
		  alert("not in a form!");
		}
		sid = n.getAttribute('name');
		if(!sid) {
		  alert("bad place for an annotation");
		} else {
		  cmd = "ScrapBook?sid=" + sid + "&amp;start=" + start + "&amp;end=" + end;
		<xsl:if test="self::node()[@mode='editor']">
		  cmd += "&amp;action=addne&amp;type=" + ne;
		</xsl:if>
		<xsl:if test="self::node()[@mode='attreditor']">
		  cmd += "&amp;action=movene&amp;neid=" + neid;
		</xsl:if>
		  cmd += "&amp;name=<xsl:value-of select="/PAPER/scrapbook/@name"/>";
		  w = window.open(cmd);
		  w.close();
		  document.location = document.location;
		}
	      }
	  </script>
	  <xsl:apply-templates/>
	</xsl:if>
      </p>
    </xsl:if>	
  </xsl:template>
  
  <xsl:template match="nebutton">
    <xsl:element name="input">
      <xsl:attribute name="type">button</xsl:attribute>
      <xsl:attribute name="value"><xsl:value-of select="@type"/></xsl:attribute>
      <xsl:attribute name="onmousedown">
	addNeAtSel("<xsl:value-of select="@type"/>")
      </xsl:attribute>
    </xsl:element>
  </xsl:template>
  
  <xsl:template match="neAttrSel">
    <select>
      <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
      <xsl:apply-templates/>
    </select>
  </xsl:template>
  
  <xsl:template match="neAttrOpt">
    <option>
      <xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
      <xsl:if test="@selected"><xsl:attribute name="selected">selected</xsl:attribute></xsl:if>
      <xsl:value-of select="@value"/>
    </option>
  </xsl:template>
  
  <xsl:template match="neTickyBox">
    <input type="checkbox">
      <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
      <xsl:if test="@selected"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
    </input>
  </xsl:template>
  
  <xsl:template match="neTextEntry">
  	<input type="text">
      <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
	  <xsl:if test="@value"><xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute></xsl:if>  	
  	</input>
  </xsl:template>
  
  <xsl:template match="neLabel">
  	<!-- <b style="color: blue" onclick="document.getElementById('relations').value+='{@name},'"> -->
  	<b style="color: blue" onclick="addRelItem('{@name}')">
  	<xsl:text> [</xsl:text><xsl:value-of select="@name"/><xsl:text>] </xsl:text></b>
  </xsl:template>
 
  <xsl:template match="char">
    <xsl:element name="span"><xsl:attribute name="ci"><xsl:value-of select="@index"/></xsl:attribute><xsl:apply-templates/></xsl:element>
  </xsl:template>
  
  <xsl:template match="attred">
    <h3><xsl:for-each select="ne"><xsl:call-template name="ne"/></xsl:for-each>
    <xsl:element name="input">
      <xsl:attribute name="type">button</xsl:attribute>
      <xsl:attribute name="value">Move to selection</xsl:attribute>
      <xsl:attribute name="onmousedown">
	moveNeToSel("<xsl:value-of select="@neid"/>")
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
    <form action="ScrapBook" method="get" name="attrform">
      <input type="hidden" name="neid"><xsl:attribute name="value"><xsl:value-of select="@neid"/></xsl:attribute></input> 
      <input type="hidden" name="sid"><xsl:attribute name="value"><xsl:value-of select="@sid"/></xsl:attribute></input> 
      <input type="hidden" name="name"><xsl:attribute name="value"><xsl:value-of select="/PAPER/scrapbook/@name"/></xsl:attribute></input>
      <input type="hidden" name="action" value="edattr"/>
      <table>
	<tbody id="attrTable">
	  <xsl:for-each select="attr"><xsl:call-template name="attr"/></xsl:for-each>
	</tbody>
      </table>
      <p>Add attribute: <input type="text" name="attrname"/><input type="button" value="Add!" onclick="addRow(document.forms['attrform'].attrname.value)"/></p>
      <input type="submit" value="Submit!"/>
    </form>
  </xsl:template>
  
  <xsl:template name="attr">
    <tr>
      <th><xsl:value-of select="@name"/></th>
      <td><input type="text">
	<xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
	<xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
      </input></td>
    </tr>
  </xsl:template>

  <!-- ***** Markup used by ScrapBook done ***** -->
  
  
  
</xsl:stylesheet>
