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
package org.hisp.dhis.keyjsonvalue;

import static java.lang.Character.isLetterOrDigit;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NamedParams;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Details of a query as it can be performed to fetch {@link KeyJsonValue}s.
 *
 * @author Jan Bernitt
 */
@ToString
@Getter
@Builder( toBuilder = true )
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public final class KeyJsonValueQuery
{

    private final String namespace;

    /**
     * By default, only entries which have at least one non-null value for the
     * extracted fields are returned. If all are included even matches with only
     * null values are included in the result list.
     */
    private final boolean includeAll;

    @Builder.Default
    private final List<Field> fields = emptyList();

    @ToString
    @Getter
    public static final class Field
    {
        /**
         * A valid path can have up to 5 levels each with an alphanumeric name
         * between 1 and 32 characters long and levels being separated by a dot.
         *
         * The path needs to be protected since it becomes part of the SQL when
         * the path is extracted from the JSON values. Therefore, the
         * limitations on the path are quite strict even if this will not allow
         * some corner case names to be used that would be valid JSON member
         * names.
         */
        private static final String PATH_PATTERN = "^[-_a-zA-Z0-9]{1,32}(?:\\.[-_a-zA-Z0-9]{1,32}){0,5}$";

        private final String path;

        private final String alias;

        public Field( String path )
        {
            this( path, null );
        }

        public Field( String path, String alias )
        {
            if ( path == null || !path.matches( PATH_PATTERN ) )
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7650, path ) );
            }
            this.path = path;
            this.alias = alias != null ? alias : path;
        }
    }

    public KeyJsonValueQuery with( NamedParams params )
    {
        String fieldsParam = params.getString( "fields", null );
        String namespaceParam = params.getString( "namespace", null );
        KeyJsonValueQueryBuilder queryBuilder = toBuilder();
        if ( fieldsParam != null )
        {
            queryBuilder = queryBuilder.fields( parseFields( fieldsParam ) );
        }
        if ( namespaceParam != null )
        {
            queryBuilder = queryBuilder.namespace( namespaceParam );
        }
        return queryBuilder.build();
    }

    /**
     * Parses the fields URL parameter form to a list of {@link Field}s.
     *
     * In text form fields can describe nested fields in two forms:
     *
     * <pre>
     *   root[child]
     *   root[child1,child2]
     *   root[level1[level2]
     * </pre>
     *
     * which is similar to the second form using dot
     *
     * <pre>
     *   root.child
     *   root.child1,root.child2
     *   root.level1.level2
     * </pre>
     *
     * E leaf in this text form can be given an alias in round braces:
     *
     * <pre>
     *   root(alias)
     *   root[child(alias)]
     * </pre>
     *
     * @param fields a comma separated list of fields
     * @return the object form of the text representation given if valid
     * @throws IllegalQueryException in case the provided text form is not valid
     */
    public static List<Field> parseFields( String fields )
    {
        final List<Field> flat = new ArrayList<>();
        final int len = fields.length();
        String parentPath = "";
        int start = 0;
        while ( start < len )
        {
            int end = findNameEnd( fields, start );
            String field = fields.substring( start, end );
            start = end + 1;
            if ( end >= len )
            {
                addNonEmptyTo( flat, parentPath, field );
                return flat;
            }
            char next = fields.charAt( end );
            if ( next == ',' )
            {
                addNonEmptyTo( flat, parentPath, field );
            }
            else if ( next == '[' )
            {
                parentPath += field + ".";
            }
            else if ( next == ']' )
            {
                addNonEmptyTo( flat, parentPath, field );
                parentPath = parentPath.substring( 0, parentPath.lastIndexOf( '.', parentPath.length() - 2 ) + 1 );
            }
            else
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E7651, end, next ) );
            }
        }
        return flat;
    }

    /**
     * Adds a {@link Field} to the provided fields list of the leave field name
     * is not empty (which would indicate that we are not at the end of a new
     * field in the parsing process).
     *
     * @param fields list of fields to add to
     * @param parent parent path (might contain dotted segments)
     * @param field leaf path (no dotted segments)
     */
    private static void addNonEmptyTo( List<Field> fields, String parent, String field )
    {
        if ( !field.isEmpty() )
        {
            int aliasStart = field.indexOf( '(' );
            String name = aliasStart > 0 ? field.substring( 0, aliasStart ) : field;
            String alias = aliasStart > 0 ? field.substring( aliasStart + 1, field.length() - 1 ) : null;
            fields.add( new Field( parent + name, alias ) );
        }
    }

    /**
     * @param fields search text
     * @param start start index in the search text
     * @return first index in the fields string that is not a valid name
     *         character starting from the start position.
     */
    private static int findNameEnd( String fields, int start )
    {
        int pos = start;
        while ( pos < fields.length() && isNameCharacter( fields.charAt( pos ) ) )
        {
            pos++;
        }
        return findAliasEnd( fields, pos );
    }

    /**
     * @param fields search text
     * @param start start position in search text
     * @return first index in the fields string that is after a potential alias.
     *         This assumes the start position must point to the start of an
     *         alias or no alias is present.
     */
    private static int findAliasEnd( String fields, int start )
    {
        if ( start >= fields.length() || fields.charAt( start ) != '(' )
        {
            return start;
        }
        return fields.indexOf( ')', start ) + 1;
    }

    private static boolean isNameCharacter( char c )
    {
        return isLetterOrDigit( c ) || c == '.';
    }
}
