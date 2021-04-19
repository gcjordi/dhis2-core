/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.opengis.geometry.primitive.Point;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "valueType", namespace = DxfNamespaces.DXF_2_0 )
public enum ValueType
{
    TEXT( String.class, true ),
    LONG_TEXT( String.class, true ),
    LETTER( String.class, true ),
    PHONE_NUMBER( String.class, false ),
    EMAIL( String.class, false ),
    BOOLEAN( Boolean.class, true ),
    TRUE_ONLY( Boolean.class, true ),
    DATE( LocalDate.class, false ),
    DATETIME( LocalDateTime.class, false ),
    TIME( String.class, false ),
    NUMBER( Double.class, true ),
    UNIT_INTERVAL( Double.class, true ),
    PERCENTAGE( Double.class, true ),
    INTEGER( Integer.class, true ),
    INTEGER_POSITIVE( Integer.class, true ),
    INTEGER_NEGATIVE( Integer.class, true ),
    INTEGER_ZERO_OR_POSITIVE( Integer.class, true ),
    TRACKER_ASSOCIATE( TrackedEntityInstance.class, false ),
    USERNAME( String.class, false ),
    COORDINATE( Point.class, true ),
    ORGANISATION_UNIT( OrganisationUnit.class, false ),
    AGE( Date.class, false ),
    URL( String.class, false ),
    FILE_RESOURCE( String.class,
        ( aggregationType ) -> aggregationType == AggregationType.COUNT,
        FileTypeValueOptions.class ),
    IMAGE( String.class, false, FileTypeValueOptions.class );

    public static final Set<ValueType> INTEGER_TYPES = ImmutableSet.of(
        INTEGER, INTEGER_POSITIVE, INTEGER_NEGATIVE, INTEGER_ZERO_OR_POSITIVE );

    public static final Set<ValueType> DECIMAL_TYPES = ImmutableSet.of(
        NUMBER, UNIT_INTERVAL, PERCENTAGE );

    public static final Set<ValueType> BOOLEAN_TYPES = ImmutableSet.of(
        BOOLEAN, TRUE_ONLY );

    public static final Set<ValueType> TEXT_TYPES = ImmutableSet.of(
        TEXT, LONG_TEXT, LETTER, TIME, USERNAME, EMAIL, PHONE_NUMBER, URL );

    public static final Set<ValueType> DATE_TYPES = ImmutableSet.of(
        DATE, DATETIME, AGE );

    public static final Set<ValueType> FILE_TYPES = ImmutableSet.of(
        FILE_RESOURCE, IMAGE );

    public static final Set<ValueType> GEO_TYPES = ImmutableSet.of(
        COORDINATE );

    public static final Set<ValueType> NUMERIC_TYPES = ImmutableSet.<ValueType> builder().addAll(
        INTEGER_TYPES ).addAll( DECIMAL_TYPES ).build();

    @Deprecated
    private final Class<?> javaClass;

    private Function<AggregationType, Boolean> aggregateable;

    private Class<? extends ValueTypeOptions> valueTypeOptionsClass;

    ValueType()
    {
        this.javaClass = null;
    }

    ValueType( Class<?> javaClass, boolean aggregateable )
    {
        this.javaClass = javaClass;
        this.aggregateable = ( aggregated ) -> aggregateable;
        this.valueTypeOptionsClass = null;
    }

    ValueType( Class<?> javaClass, Function<AggregationType, Boolean> aggregateable )
    {
        this.javaClass = javaClass;
        this.aggregateable = aggregateable;
        this.valueTypeOptionsClass = null;
    }

    ValueType( Class<?> javaClass, boolean aggregateable, Class<? extends ValueTypeOptions> valueTypeOptionsClass )
    {
        this( javaClass, aggregateable );
        this.valueTypeOptionsClass = valueTypeOptionsClass;
    }

    ValueType( Class<?> javaClass, Function<AggregationType, Boolean> aggregateable,
        Class<? extends ValueTypeOptions> valueTypeOptionsClass )
    {
        this( javaClass, aggregateable );
        this.valueTypeOptionsClass = valueTypeOptionsClass;
    }

    public Class<?> getJavaClass()
    {
        return javaClass;
    }

    public boolean isInteger()
    {
        return INTEGER_TYPES.contains( this );
    }

    public boolean isDecimal()
    {
        return DECIMAL_TYPES.contains( this );
    }

    public boolean isBoolean()
    {
        return BOOLEAN_TYPES.contains( this );
    }

    public boolean isText()
    {
        return TEXT_TYPES.contains( this );
    }

    public boolean isDate()
    {
        return DATE_TYPES.contains( this );
    }

    public boolean isFile()
    {
        return FILE_TYPES.contains( this );
    }

    public boolean isGeo()
    {
        return GEO_TYPES.contains( this );
    }

    public boolean isOrganisationUnit()
    {
        return ORGANISATION_UNIT == this;
    }

    /**
     * Includes integer and decimal types.
     */
    public boolean isNumeric()
    {
        return NUMERIC_TYPES.contains( this );
    }

    public boolean isAggregateable( AggregationType aggregationType )
    {
        return aggregateable.apply( aggregationType );
    }

    public Class<? extends ValueTypeOptions> getValueTypeOptionsClass()
    {
        return this.valueTypeOptionsClass;
    }

    /**
     * Returns a simplified value type. As an example, if the value type is any
     * numeric type such as integer, percentage, then {@link ValueType#NUMBER}
     * is returned. Can return any of:
     *
     * <ul>
     * <li>{@link ValueType#NUMBER} for any numeric types.</li>
     * <li>{@link ValueType#BOOLEAN} for any boolean types.</li>
     * <li>{@link ValueType#DATE} for any date / time types.</li>
     * <li>{@link ValueType#FILE_RESOURCE} for any file types.</li>
     * <li>{@link ValueType#COORDINATE} for any geometry types.</li>
     * <li>{@link ValueType#TEXT} for any textual types.</li>
     * </ul>
     *
     * @return a simplified value type.
     */
    public ValueType toSimplifiedValueType()
    {
        if ( isNumeric() )
        {
            return ValueType.NUMBER;
        }
        else if ( isBoolean() )
        {
            return ValueType.BOOLEAN;
        }
        else if ( isDate() )
        {
            return ValueType.DATE;
        }
        else if ( isFile() )
        {
            return ValueType.FILE_RESOURCE;
        }
        else if ( isGeo() )
        {
            return ValueType.COORDINATE;
        }
        else
        {
            return ValueType.TEXT;
        }
    }

    public static ValueType fromString( String valueType )
        throws IllegalArgumentException
    {
        return Arrays.stream( ValueType.values() ).filter( v -> v.toString().equals( valueType ) ).findFirst()
            .orElseThrow( () -> new IllegalArgumentException( "unknown value: " + valueType ) );
    }

    public static Set<ValueType> getAggregateables( AggregationType aggregationType )
    {
        return Arrays.stream( ValueType.values() ).filter( v -> v.aggregateable.apply( aggregationType ) )
            .collect( toSet() );
    }
}
