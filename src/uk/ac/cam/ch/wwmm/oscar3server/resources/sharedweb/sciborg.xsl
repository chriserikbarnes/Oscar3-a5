<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cml="http://www.xml-cml.org/schema">
	<xsl:output method="html" indent="yes" />
	
	<xsl:template match="/">
		<html>
			<head><title>SciBorg</title></head>
			<body>
				<h1>SciBorg</h1>
				<p><a href="/SciBorg/papers">View all papers</a></p>
				<xsl:apply-templates/>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="papers">
		<ul>
			<xsl:for-each select="paper">
				<li><xsl:call-template name="paper"/></li>
			</xsl:for-each>
		</ul>
	</xsl:template>

	<xsl:template match="paper" name="paper">
		<a href="/SciBorg/papers/{@id}"><xsl:value-of select="@title"/></a>
		<xsl:text> </xsl:text><a href="/ViewPaper/{@shortpath}">(view)</a>
		<ul>
			<xsl:for-each select="sentence">
				<li><xsl:call-template name="sentence"/></li>
			</xsl:for-each>
		</ul>
	</xsl:template>

	<xsl:template match="sentence" name="sentence">
		<a href="/SciBorg/sentences/{@id}">
			<xsl:value-of select="@content" />
		</a> <a href="/SciBorg/papers/{@docid}">(back to paper)</a>
		<ul>
			<xsl:for-each select="rmrs">
				<li>
					<xsl:call-template name="rmrs" />
				</li>
			</xsl:for-each>
		</ul>
		<ul>
			<xsl:for-each select="ne">
				<li>
					<xsl:call-template name="ne" />
				</li>
			</xsl:for-each>
		</ul>
	</xsl:template>

	<xsl:template match="ne" name="ne">
		<a href="/SciBorg/nes/{@id}"><xsl:value-of select="@value"/></a>
	</xsl:template>

	<xsl:template match="rmrs[@id]" name="rmrs">
		<a href="/SciBorg/rmrs/{@id}">RMRS <xsl:value-of select="@id"/> (<xsl:value-of select="@type"/>)</a>
		<xsl:text> </xsl:text><a href="/SciBorg/sentences/{@sentenceid}">(sentence)</a>
		<xsl:for-each select="rmrs">
			<xsl:call-template name="rmrsdetail"/>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="rmrs[@cfrom]" name="rmrsdetail">
		<p>
		LTOP : l<xsl:value-of select="label/@vid"/><br/>
		<xsl:for-each select="ep">
			l<xsl:value-of select="label/@vid"/>:
			<xsl:if test="anchor">a<xsl:value-of select="anchor/@vid"/>:</xsl:if>
			<xsl:choose>
				<xsl:when test="realpred">
					_<xsl:value-of select="realpred/@lemma"/>_<xsl:value-of select="realpred/@pos"/>
					<xsl:if test="realpred/@sense">_<xsl:value-of select="realpred/@sense"/></xsl:if>
				</xsl:when>
				<xsl:when test="gpred">
					<xsl:text> </xsl:text><xsl:value-of select="gpred"/>
				</xsl:when>
			</xsl:choose>(<xsl:value-of select="var/@sort"/><xsl:value-of select="var/@vid"/>)
			<br/>
		</xsl:for-each>
		<xsl:choose>
			<xsl:when test="ep/anchor">
				<xsl:for-each select="rarg">
					a<xsl:value-of select="label/@vid"/>: <xsl:value-of select="rargname"/>(<xsl:choose>
					<xsl:when test="var"><xsl:value-of select="var/@sort"/><xsl:value-of select="var/@vid"/></xsl:when>
					<xsl:when test="constant"><xsl:value-of select="constant"/></xsl:when>
					</xsl:choose>)
					<br/>		
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="rarg">
					l<xsl:value-of select="label/@vid"/>: <xsl:value-of select="rargname"/>(<xsl:choose>
					<xsl:when test="var"><xsl:value-of select="var/@sort"/><xsl:value-of select="var/@vid"/></xsl:when>
					<xsl:when test="constant"><xsl:value-of select="constant"/></xsl:when>
					</xsl:choose>)
					<br/>		
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="ing">
			<xsl:value-of select="ing-a/var/@sort"/><xsl:value-of select="ing-a/var/@vid"/>			
			in-g
			<xsl:value-of select="ing-b/var/@sort"/><xsl:value-of select="ing-b/var/@vid"/>
			<br/>
		</xsl:for-each>
		<xsl:for-each select="hcons[@hreln='qeq']">
			<xsl:value-of select="hi/var/@sort"/><xsl:value-of select="hi/var/@vid"/>
			qeq			
			l<xsl:value-of select="lo/label/@vid"/>
			<br/>
		</xsl:for-each>
		</p>
	</xsl:template>

</xsl:stylesheet>