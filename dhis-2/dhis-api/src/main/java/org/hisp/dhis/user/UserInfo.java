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
package org.hisp.dhis.user;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents minimal user information.
 *
 * @author Lars Helge Overland
 */
public class UserInfo
{
    private long id;

    private String username;

    private Set<String> authorities = new HashSet<>();

    protected UserInfo()
    {
    }

    public UserInfo( long id, String username, Set<String> authorities )
    {
        this.id = id;
        this.username = username;
        this.authorities = authorities;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean isSuper()
    {
        return authorities.contains( UserAuthorityGroup.AUTHORITY_ALL );
    }

    public static UserInfo fromUser( User user )
    {
        if ( user == null )
        {
            return null;
        }

        UserCredentials credentials = user.getUserCredentials();

        return new UserInfo( credentials.getId(), credentials.getUsername(), credentials.getAllAuthorities() );
    }

    // -------------------------------------------------------------------------
    // Get methods
    // -------------------------------------------------------------------------

    public long getId()
    {
        return id;
    }

    public String getUsername()
    {
        return username;
    }

    public Set<String> getAuthorities()
    {
        return authorities;
    }
}
