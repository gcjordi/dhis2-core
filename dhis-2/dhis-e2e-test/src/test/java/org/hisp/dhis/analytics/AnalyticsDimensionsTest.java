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

package org.hisp.dhis.analytics;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.actions.metadata.ProgramActions;
import org.hisp.dhis.actions.metadata.ProgramStageActions;
import org.hisp.dhis.actions.metadata.TrackedEntityAttributeActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.Program;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.matchers.CustomMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AnalyticsDimensionsTest
    extends ApiTest
{
    private Program trackerProgram = Constants.TRACKER_PROGRAM;

    private String trackerProgramStage = trackerProgram.getProgramStages().get( 0 );

    private AnalyticsEnrollmentsActions analyticsEnrollmentsActions;

    private AnalyticsEventActions analyticsEventActions;

    private TrackedEntityAttributeActions trackedEntityAttributeActions;

    private ProgramActions programActions;

    @BeforeAll
    public void beforeAll()
    {
        trackedEntityAttributeActions = new TrackedEntityAttributeActions();
        programActions = new ProgramActions();
        analyticsEnrollmentsActions = new AnalyticsEnrollmentsActions();
        analyticsEventActions = new AnalyticsEventActions();
    }

    @ValueSource( strings = { "name:desc", "code:desc", "uid:asc", "lastUpdated:desc", "created:asc" } )
    @ParameterizedTest
    public void shouldOrder( String order )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder()
            .add( "order", order );

        analyticsEnrollmentsActions.query().getDimensions( trackerProgram.getUid(), queryParamsBuilder )
            .validate()
            .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) );

        analyticsEventActions.query().getDimensions( trackerProgram.getProgramStages().get( 0 ), queryParamsBuilder )
            .validate()
            .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) );
    }

    @Test
    public void shouldReturnDataElementsFromAllStages()
    {
        analyticsEnrollmentsActions.query().getDimensionsByDimensionType( trackerProgram.getUid(), "DATA_ELEMENT" )
            .validate()
            .body( "dimensions.id", everyItem( CustomMatchers.startsWithOneOf( trackerProgram.getProgramStages() ) ) );
    }

    @ValueSource( strings = {
        "DATA_ELEMENT", "PROGRAM_INDICATOR", "PROGRAM_ATTRIBUTE"
    } )
    @ParameterizedTest
    public void shouldFilterByDimensionType( String dimensionType )
    {
        analyticsEnrollmentsActions.query().getDimensionsByDimensionType( trackerProgram.getUid(), dimensionType )
            .validate()
            .body( "dimensions", notNullValue() )
            .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "dimensions.dimensionType", everyItem( equalTo( dimensionType ) ) );
    }

    @Test
    public void shouldOnlyReturnProgramTrackedEntityAttributes()
    {
        String teaNotAssignedToProgram = trackedEntityAttributeActions.create( "TEXT" );

        analyticsEnrollmentsActions.query().getDimensionsByDimensionType( trackerProgram.getUid(), "PROGRAM_ATTRIBUTE" )
            .validate()
            .body( "dimensions.uid", not( hasItem( equalTo( teaNotAssignedToProgram ) ) ) );

        analyticsEnrollmentsActions.aggregate().getDimensionsByDimensionType( trackerProgram.getUid(), "PROGRAM_ATTRIBUTE" )
            .validate()
            .body( "dimensions.uid", not( hasItem( equalTo( teaNotAssignedToProgram ) ) ) );

        analyticsEventActions.aggregate().getDimensions( trackerProgram.getProgramStages().get( 0 ) )
            .validate()
            .body( "dimensions.uid", not( hasItem( equalTo( teaNotAssignedToProgram ) ) ) );
    }

    @Test
    public void shouldOnlyReturnConfidentialAttributeInAggregateDimensions()
    {
        String confidentialAttribute = trackedEntityAttributeActions.create( "NUMBER", true, true );
        programActions.addAttribute( Constants.TRACKER_PROGRAM_ID, confidentialAttribute, false ).validateStatus( 200 );

        analyticsEnrollmentsActions.query().getDimensionsByDimensionType( trackerProgram.getUid(), "PROGRAM_ATTRIBUTE" )
            .validate()
            .body( "dimensions.uid", not( CoreMatchers.hasItem( confidentialAttribute ) ) );

        analyticsEnrollmentsActions.aggregate().getDimensionsByDimensionType( trackerProgram.getUid(), "PROGRAM_ATTRIBUTE" )
            .validate()
            .body( "dimensions.uid", CoreMatchers.hasItem( confidentialAttribute ) );
    }

    @ValueSource( strings = { "DATA_ELEMENT", "PROGRAM_ATTRIBUTE" } )
    @ParameterizedTest
    public void shouldLimitAggregateDimensionsByValueTypes( String dimensionType )
    {
        List<String> acceptedValueTypes = Arrays
            .asList( "NUMBER", "UNIT_INTERVAL", "PERCENTAGE", "INTEGER", "INTEGER_POSITIVE", "INTEGER_NEGATIVE",
                "INTEGER_ZERO_OR_POSITIVE", "BOOLEAN", "TRUE_ONLY" );

        Consumer<ApiResponse> validate = response -> {
            response.validate()
                .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) )
                .body( "dimensions.valueType", Matchers.everyItem( in( acceptedValueTypes ) ) );
        };

        validate.accept(
            analyticsEnrollmentsActions.aggregate().getDimensionsByDimensionType( trackerProgram.getUid(), dimensionType ) );
        validate.accept( analyticsEventActions.aggregate().getDimensions( trackerProgram.getProgramStages().get( 0 ) ) );
    }

    @Test
    public void shouldOnlyReturnDataElementsAssociatedWithProgramStage()
    {
        List<String> dataElements = new ProgramStageActions()
            .get( String.format( "/%s/programStageDataElements/gist?fields=dataElement", trackerProgramStage ) )
            .extractList( "programStageDataElements.dataElement" );

        analyticsEventActions.query().getDimensionsByDimensionType( trackerProgramStage, "DATA_ELEMENT" )
            .validate()
            .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) )
            .body( "dimensions.id", everyItem( in( dataElements ) ) );
    }

    @Test
    public void shouldReturnAssociatedCategoriesWhenProgramHasCatCombo()
    {
        String programWithCatCombo = programActions
            .get( "?filter=categoryCombo.code:!eq:default&filter=programType:eq:WITH_REGISTRATION" )
            .extractString( "programs.id[0]" );

        String programStage = programActions.get( programWithCatCombo + "/programStages" )
            .extractString( "programStages[0].id" );

        assertNotNull( programStage );

        Consumer<ApiResponse> validate = response -> {
            response.validate()
                .body( "dimensions", hasSize( greaterThanOrEqualTo( 1 ) ) )
                .body( "dimensions.dimensionType", everyItem( startsWith( "CATEGORY" ) ) )
                .body( "dimensions.dimensionType", hasItems( "CATEGORY", "CATEGORY_OPTION_GROUP_SET" ) );
        };

        validate.accept( analyticsEventActions.aggregate()
            .getDimensions( programStage, new QueryParamsBuilder().add( "filter", "dimensionType:like:CATEGORY" ) ) );

        validate.accept( analyticsEventActions.query()
            .getDimensions( programStage, new QueryParamsBuilder().add( "filter", "dimensionType:like:CATEGORY" ) ) );

    }
}
