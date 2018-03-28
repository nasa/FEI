<xsl:stylesheet	
 xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
 version='1.0'>
<xsl:output method='text' indent = 'no'/>
<xsl:strip-space elements = 'arg'/>
<xsl:decimal-format decimal-separator='.' grouping-separator=',' />

<!-- Komodo XML Style Sheet by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov -->

<xsl:template match='KomodoDbStmts'>
  <!-- For each Statment create its part -->
  <xsl:for-each select='stmt'>
    <xsl:sort select='@name'/>
    <xsl:apply-templates select='.' mode = 'print'/>
  </xsl:for-each>
</xsl:template>

<xsl:template match='stmt' mode = 'print'>
  <xsl:apply-templates select = 'sql' mode = 'print'/>
</xsl:template>

<xsl:template match = 'sql' mode = 'print'>
  <xsl:apply-templates select = 'cmd' mode = 'print'/>
  <xsl:apply-templates select = 'args' mode = 'print'/>
</xsl:template>

<xsl:template match = 'cmd' mode = 'print'>
  <xsl:value-of select = 'normalize-space(.)' />
</xsl:template>

<xsl:template match = 'args' mode = 'print'>
  <xsl:apply-templates select = 'arg' mode = 'print'/>
</xsl:template>
	
<xsl:template match = 'arg' mode = 'print'>
  <xsl:variable name='opt' select='@optional'/>
  <xsl:variable name='val' select='.'/>
  <xsl:text>arg:</xsl:text>
  <xsl:if test = "$opt = 'no'"> 
    <xsl:text>r</xsl:text>
  </xsl:if>
  <xsl:if test = "$opt = 'yes'">
    <xsl:if test = "not($opt = null)">
      <xsl:text>o</xsl:text>
    </xsl:if>
  </xsl:if>
  <xsl:if test = "$val = 'string'">
    <xsl:text>s</xsl:text>
  </xsl:if>
  <xsl:if test = "$val = 'numeral'">
    <xsl:text>n</xsl:text>
  </xsl:if>
  <xsl:if test = "$val = 'bit'">
    <xsl:text>b</xsl:text>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
