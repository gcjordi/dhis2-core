/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.common;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.replaceOnce;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Lars Helge Overland
 */
@Getter
@RequiredArgsConstructor
public enum QueryOperator
{
    EQ( "=", true ),
    GT( ">" ),
    GE( ">=" ),
    LT( "<" ),
    LE( "<=" ),
    LIKE( "like" ),
    IN( "in", true ),
    // Tracker new operators
    STARTS( "starts" ),
    ENDS( "ends" ),
    // Analytics specifics
    IEQ( "==", true ),
    NE( "!=", true ),
    NIEQ( "!==", true ),
    NLIKE( "not like" ),
    ILIKE( "ilike" ),
    NILIKE( "not ilike" );

    private final String value;

    private final boolean nullAllowed;

    QueryOperator( String value )
    {
        this.value = value;
        this.nullAllowed = false;
    }

    public static QueryOperator fromString( String string )
    {
        if ( string == null || string.isEmpty() )
        {
            return null;
        }

        if ( string.trim().startsWith( "!" ) )
        {
            return valueOf( "N" + replaceOnce( string, "!", EMPTY ).toUpperCase() );
        }

        return valueOf( string.toUpperCase() );
    }

}
