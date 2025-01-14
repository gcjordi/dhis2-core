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
package org.hisp.dhis.actions.tracker.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;

import java.io.File;
import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerActions
    extends RestApiActions
{
    private Logger logger = Logger.getLogger( TrackerActions.class.getName() );

    public TrackerActions()
    {
        super( "/tracker" );
    }

    public ApiResponse getJob( String jobId )
    {
        return this.get( "/jobs/" + jobId );
    }

    public ApiResponse waitUntilJobIsCompleted( String jobId )
    {
        logger.info( String.format( "Waiting until tracker job with id %s is completed", jobId ) );
        ApiResponse response = null;
        boolean completed = false;
        int maxAttempts = 100;

        while ( !completed && maxAttempts > 0 )
        {
            response = getJob( jobId );
            response.validate().statusCode( 200 );
            completed = response.extractList( "completed" ).contains( true );
            maxAttempts--;
        }

        if ( maxAttempts == 0 )
        {
            logger.warning(
                String.format( "Tracker job didn't complete in %d. Message: %s", maxAttempts,
                    response.extract( "message" ) ) );
        }

        logger.info( "Tracker job is completed. Message: " + response.extract( "message" ) );
        return response;
    }

    public TrackerApiResponse postAndGetJobReport( File file )
    {
        ApiResponse response = this.postFile( file );

        return getJobReportByImportResponse( response );
    }

    public TrackerApiResponse postAndGetJobReport( File file, QueryParamsBuilder queryParamsBuilder )
    {
        ApiResponse response = this.postFile( file, queryParamsBuilder );

        return getJobReportByImportResponse( response );
    }

    public TrackerApiResponse postAndGetJobReport( JsonObject jsonObject )
    {
        ApiResponse response = this.post( jsonObject );

        return getJobReportByImportResponse( response );
    }

    public TrackerApiResponse postAndGetJobReport( JsonObject jsonObject, QueryParamsBuilder queryParamsBuilder )
    {
        ApiResponse response = this.post( jsonObject, queryParamsBuilder );

        return getJobReportByImportResponse( response );
    }

    public TrackerApiResponse getJobReport( String jobId, String reportMode )
    {
        ApiResponse response = this.get( String.format( "/jobs/%s/report?reportMode=%s", jobId, reportMode ) );

        // add created entities

        saveCreatedData( response );
        return new TrackerApiResponse( response );
    }

    public TrackerApiResponse getTrackedEntity( String entityId )
    {
        return getTrackedEntity( entityId, new QueryParamsBuilder());
    }

    public TrackerApiResponse getTrackedEntity( String entityId, QueryParamsBuilder queryParamsBuilder )
    {
        return new TrackerApiResponse( this.get( "/trackedEntities/" + entityId , queryParamsBuilder ));
    }

    public TrackerApiResponse getTrackedEntities( QueryParamsBuilder queryParamsBuilder )
    {
        return new TrackerApiResponse( this.get( "/trackedEntities/", queryParamsBuilder ) );
    }

    public TrackerApiResponse getEnrollment( String enrollmentId )
    {
        return new TrackerApiResponse( this.get( "/enrollments/" + enrollmentId ) );
    }

    public TrackerApiResponse getEvent( String eventId )
    {
        return new TrackerApiResponse( this.get( "/events/" + eventId ) );
    }

    private void saveCreatedData( ApiResponse response )
    {
        String[] val = {
            "TRACKED_ENTITY,/trackedEntityInstances",
            "EVENT,/events",
            "ENROLLMENT,/enrollments",
            "RELATIONSHIP,/relationships"
        };

        for ( String s : val )
        {
            String path = String.format( "bundleReport.typeReportMap.%s.objectReports.uid", s.split( "," )[0] );

            if ( response.extractList( path ) != null )
            {
                response.extractList( path ).stream()
                    .filter( o -> o != null )
                    .forEach( id -> {
                        this.addCreatedEntity( s.split( "," )[1], id.toString() );
                    } );
            }
        }

    }

    private TrackerApiResponse getJobReportByImportResponse( ApiResponse response )
    {
        // if import is sync, just return response
        if ( response.extractString( "response.id" ) == null )
        {
            return new TrackerApiResponse( response );
        }

        response.validate()
            .statusCode( 200 )
            .body( "response.id", notNullValue() );

        String jobId = response.extractString( "response.id" );

        this.waitUntilJobIsCompleted( jobId );

        return this.getJobReport( jobId, "FULL" );
    }
}
