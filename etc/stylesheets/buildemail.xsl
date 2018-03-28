<?xml version="1.0"?>
<!--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>

    <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>
    <xsl:variable name="tasklist" select="/cruisecontrol/build//target/task"/>
    <xsl:variable name="javadoc.tasklist" select="$tasklist[@name='Javadoc'] | $tasklist[@name='javadoc']"/>
    <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>
    <xsl:variable name="jar.tasklist" select="$tasklist[@name='Jar']/message[@priority='info'] | $tasklist[@name='jar']/message[@priority='info']"/>
    <xsl:variable name="war.tasklist" select="$tasklist[@name='War']/message[@priority='info'] | $tasklist[@name='war']/message[@priority='info']"/>
    <xsl:variable name="dist.count" select="count($jar.tasklist) + count($war.tasklist)"/>


    <xsl:template match="/">
        <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>
        <xsl:variable name="tasklist" select="/cruisecontrol/build//target/task"/>
        <xsl:variable name="javadoc.tasklist" select="$tasklist[@name='Javadoc'] | $tasklist[@name='javadoc']"/>
        <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>
        <xsl:variable name="jar.tasklist" select="$tasklist[@name='Jar']/message[@priority='info'] | $tasklist[@name='jar']/message[@priority='info']"/>
        <xsl:variable name="war.tasklist" select="$tasklist[@name='War']/message[@priority='info'] | $tasklist[@name='war']/message[@priority='info']"/>
        <xsl:variable name="dist.count" select="count($jar.tasklist) + count($war.tasklist)"/>


        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
          <tr><td class="header-data">View build results here -&gt; <a href="http://sorrento.jpl.nasa.gov:7108/build/">http://sorrento.jpl.nasa.gov:7108/build/</a><p /></td></tr>

            <xsl:if test="cruisecontrol/build/@error">
                <tr><td class="header-title">BUILD FAILED</td></tr>
                <tr><td class="header-data">
                    <span class="header-label">Ant Error Message:&#160;</span>
                    <xsl:value-of select="cruisecontrol/build/@error"/>
                </td></tr>
            </xsl:if>

            <xsl:if test="not (cruisecontrol/build/@error)">
                <tr><td class="header-title">BUILD COMPLETE&#160;-&#160;
                    <xsl:value-of select="cruisecontrol/info/property[@name='label']/@value"/>
                </td></tr>
            </xsl:if>

            <tr><td class="header-data">
                <span class="header-label">Date of build:&#160;</span>
                <xsl:value-of select="cruisecontrol/info/property[@name='builddate']/@value"/>
            </td></tr>
            <tr><td class="header-data">
                <span class="header-label">Time to build:&#160;</span>
                <xsl:value-of select="cruisecontrol/build/@time"/>
            </td></tr>
            <xsl:apply-templates select="$modification.list">
                <xsl:sort select="date" order="descending" data-type="text" />
            </xsl:apply-templates>

	    <xsl:apply-templates select="cruisecontrol/build" />
            <xsl:apply-templates select="cruisecontrol/testsuites" />
            <xsl:apply-templates select="$javadoc.tasklist" />
            <xsl:apply-templates select="$modification.list" />

        </table>
    </xsl:template>

    <!-- Last Modification template -->
    <xsl:template match="modification">
        <xsl:if test="position() = 1">
            <tr><td class="header-data">
                <span class="header-label">Last changed:&#160;</span>
                <xsl:value-of select="date"/>
            </td></tr>
            <tr><td class="header-data">
                <span class="header-label">Last log entry:&#160;</span>
                <xsl:value-of select="comment"/>
            </td></tr>
        </xsl:if>
    </xsl:template>

    
    <xsl:variable name="tasklist" select="/cruisecontrol/build//target/task"/>
    <xsl:variable name="javac.tasklist" select="$tasklist[@name='Javac'] | $tasklist[@name='javac']"/>
    <xsl:variable name="ejbjar.tasklist" select="$tasklist[@name='EjbJar'] | $tasklist[@name='ejbjar']"/>

    <xsl:template match="cruisecontrol/build">

        <xsl:variable name="javac.error.messages" select="$javac.tasklist/message[@priority='error']"/>
        <xsl:variable name="javac.warn.messages" select="$javac.tasklist/message[@priority='warn']"/>
        <xsl:variable name="ejbjar.error.messages" select="$ejbjar.tasklist/message[@priority='error']"/>
        <xsl:variable name="ejbjar.warn.messages" select="$ejbjar.tasklist/message[@priority='warn']"/>
        <xsl:variable name="total.errorMessage.count" select="count($javac.warn.messages) + count($ejbjar.warn.messages) + count($javac.error.messages) + count($ejbjar.error.messages)"/>

        <xsl:if test="$total.errorMessage.count > 0">
            <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
                <tr>
                    <!-- NOTE: total.errorMessage.count is actually the number of lines of error
                     messages. This accurately represents the number of errors ONLY if the Ant property
                     build.compiler.emacs is set to "true" -->
                    <td class="compile-sectionheader">
                        &#160;Errors/Warnings: (<xsl:value-of select="$total.errorMessage.count"/>)
                    </td>
                </tr>
                <xsl:if test="count($javac.error.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-error-data">
                            <xsl:apply-templates select="$javac.error.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
                <xsl:if test="count($javac.warn.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-data">
                            <xsl:apply-templates select="$javac.warn.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
                <xsl:if test="count($ejbjar.error.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-error-data">
                            <xsl:apply-templates select="$ejbjar.error.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
                <xsl:if test="count($ejbjar.warn.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-warn-data">
                            <xsl:apply-templates select="$ejbjar.warn.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
            </table>
        </xsl:if>

    </xsl:template>

    <xsl:template match="message[@priority='error']">
        <xsl:value-of select="text()"/>
        <xsl:if test="count(./../message[@priority='error']) != position()">
            <br class="none"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="message[@priority='warn']">
        <xsl:value-of select="text()"/><br class="none"/>
    </xsl:template>

    <xsl:variable name="testsuite.list" select="//testsuite"/>
    <xsl:variable name="testsuite.error.count" select="count($testsuite.list/error)"/>
    <xsl:variable name="testcase.list" select="$testsuite.list/testcase"/>
    <xsl:variable name="testcase.error.list" select="$testcase.list/error"/>
    <xsl:variable name="testcase.failure.list" select="$testcase.list/failure"/>
    <xsl:variable name="totalErrorsAndFailures" select="count($testcase.error.list) + count($testcase.failure.list)"/>

    <xsl:template match="cruisecontrol/testsuites">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">

            <!-- Unit Tests -->
            <tr>
                <td class="unittests-sectionheader" colspan="4">
                   &#160;Unit Tests: (<xsl:value-of select="count($testcase.list)"/>)
                </td>
            </tr>

            <xsl:choose>
                <xsl:when test="count($testsuite.list) = 0">
                    <tr>
                        <td colspan="2" class="unittests-data">
                            No Tests Run
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" class="unittests-error">
                            This project doesn't have any tests
                        </td>
                    </tr>
                </xsl:when>

                <xsl:when test="$totalErrorsAndFailures = 0">
                    <tr>
                        <td colspan="2" class="unittests-data">
                            All Tests Passed
                        </td>
                    </tr>
                </xsl:when>
            </xsl:choose>
            <tr>
              <td>
       	         <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
            	    <xsl:apply-templates select="$testcase.error.list"/>
            	    <xsl:apply-templates select="$testcase.failure.list"/>
                 </table>
              </td>
            </tr>
            <tr/>
            <tr><td colspan="2">&#160;</td></tr>

            <xsl:if test="$totalErrorsAndFailures > 0">

              <tr>
                <td class="unittests-sectionheader" colspan="4">
                    &#160;Unit Test Error Details:&#160;(<xsl:value-of select="$totalErrorsAndFailures"/>)
                </td>
              </tr>

              <!-- (PENDING) Why doesn't this work if set up as variables up top? -->
              <xsl:call-template name="testdetail">
                <xsl:with-param name="detailnodes" select="//testsuite/testcase[.//error]"/>
              </xsl:call-template>

              <xsl:call-template name="testdetail">
                <xsl:with-param name="detailnodes" select="//testsuite/testcase[.//failure]"/>
              </xsl:call-template>


              <tr><td colspan="2">&#160;</td></tr>
            </xsl:if>


        </table>
    </xsl:template>

    <!-- UnitTest Errors -->
    <xsl:template match="error">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="class">unittests-oddrow</xsl:attribute>
            </xsl:if>

            <td class="unittests-data">
                error
            </td>
            <td class="unittests-data" width="40%">
                <xsl:value-of select="../@name"/>
            </td>
            <td class="unittests-data" width="40%">
                <xsl:value-of select="..//..//@name"/>
            </td>
        </tr>
    </xsl:template>

    <!-- UnitTest Failures -->
    <xsl:template match="failure">
        <tr>
            <xsl:if test="($testsuite.error.count + position()) mod 2 = 0">
                <xsl:attribute name="class">unittests-oddrow</xsl:attribute>
            </xsl:if>

            <td class="unittests-data">
                failure
            </td>
            <td class="unittests-data" width="40%">
                <xsl:value-of select="../@name"/>
            </td>
            <td class="unittests-data" width="40%">
                <xsl:value-of select="..//..//@name"/>
            </td>
        </tr>
    </xsl:template>

    <!-- UnitTest Errors And Failures Detail Template -->
    <xsl:template name="testdetail">
      <xsl:param name="detailnodes"/>

      <xsl:for-each select="$detailnodes">

        <tr>
            <td colspan="2" class="unittests-data">
                Test:&#160;<xsl:value-of select="@name"/>
            </td>
        </tr>
        <tr>
            <td colspan="2" class="unittests-data">
                Class:&#160;<xsl:value-of select="..//@name"/>
            </td>
        </tr>

        <xsl:if test="error">
            <tr>
                <td colspan="2" class="unittests-data">
                    Type: <xsl:value-of select="error/@type" />
                </td>
            </tr>
        <tr>
            <td colspan="2" class="unittests-data">
                Message: <xsl:value-of select="error/@message" />
            </td>
        </tr>

        <tr>
            <td colspan="2" class="unittests-error">
                <PRE>
                    <xsl:call-template name="br-replace">
                        <xsl:with-param name="word" select="error" />
                    </xsl:call-template>
                </PRE>
            </td>
        </tr>
        </xsl:if>

        <xsl:if test="failure">
        <tr>
            <td colspan="2" class="unittests-data">
                Type: <xsl:value-of select="failure/@type" />
            </td>
        </tr>
        <tr>
            <td colspan="2" class="unittests-data">
                Message: <xsl:value-of select="failure/@message" />
            </td>
        </tr>

        <tr>
            <td colspan="2" class="unittests-error">
                <pre>
                    <xsl:call-template name="br-replace">
                        <xsl:with-param name="word" select="failure"/>
                    </xsl:call-template>
                </pre>
            </td>
        </tr>
        </xsl:if>

      </xsl:for-each>
    </xsl:template>

    <xsl:template name="br-replace">
        <xsl:param name="word"/>
<!-- </xsl:text> on next line on purpose to get newline -->
<xsl:variable name="cr"><xsl:text>
</xsl:text></xsl:variable>
        <xsl:choose>
            <xsl:when test="contains($word,$cr)">
                <xsl:value-of select="substring-before($word,$cr)"/>
                <br/>
                <xsl:call-template name="br-replace">
                <xsl:with-param name="word" select="substring-after($word,$cr)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$word"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/cruisecontrol/build//target/task[@name=Javadoc]">

        <xsl:variable name="javadoc.error.messages" select="$javadoc.tasklist/message[@priority='error']"/>
        <xsl:variable name="javadoc.warn.messages" select="$javadoc.tasklist/message[@priority='warn']"/>
        <xsl:variable name="total.errorMessage.count" select="count($javadoc.warn.messages)  + count($javadoc.error.messages)"/>

        <xsl:if test="$total.errorMessage.count > 0">
            <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
                <tr>
                    <!-- NOTE: total.errorMessage.count is actually the number of lines of error
                     messages. This accurately represents the number of errors ONLY if the Ant property
                     build.compiler.emacs is set to "true" -->
                    <td class="compile-sectionheader">
                        &#160;Javadoc Errors/Warnings: (<xsl:value-of select="$total.errorMessage.count"/>)
                    </td>
                </tr>
                <xsl:if test="count($javadoc.error.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-error-data">
                            <xsl:apply-templates select="$javadoc.error.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
                <xsl:if test="count($javadoc.warn.messages) > 0">
                    <tr>
                        <td>
                           <pre class="compile-data">
                            <xsl:apply-templates select="$javadoc.warn.messages"/>
                           </pre>
                        </td>
                    </tr>
                </xsl:if>
            </table>
        </xsl:if>

    </xsl:template>

    <xsl:template match="message[@priority='error']">
        <xsl:value-of select="text()"/>
        <xsl:if test="count(./../message[@priority='error']) != position()">
            <br/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="message[@priority='warn']">
        <xsl:value-of select="text()"/><br/>
    </xsl:template>


    <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>

    <xsl:template match="modification">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
            <!-- Modifications -->
            <tr>
                <td class="modifications-sectionheader" colspan="4">
                    &#160;Modifications since last build:&#160;
                    (<xsl:value-of select="count($modification.list)"/>)
                </td>
            </tr>

            <xsl:apply-templates select="$modification.list">
                <xsl:sort select="date" order="descending" data-type="text" />
            </xsl:apply-templates>
            
        </table>
    </xsl:template>

    <!-- Modifications template -->
    <xsl:template match="modification">
        <tr>
            <xsl:if test="position() mod 2=0">
                <xsl:attribute name="class">modifications-oddrow</xsl:attribute>
            </xsl:if>
            <xsl:if test="position() mod 2!=0">
                <xsl:attribute name="class">modifications-evenrow</xsl:attribute>
            </xsl:if>

            <td class="modifications-data"><xsl:value-of select="@type"/></td>
            <td class="modifications-data"><xsl:value-of select="user"/></td>
            <td class="modifications-data"><xsl:value-of select="project"/>/<xsl:value-of select="filename"/></td>
            <td class="modifications-data"><xsl:value-of select="comment"/></td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
