/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics.workspaces;

import com.codenvy.analytics.metrics.*;

import javax.annotation.security.RolesAllowed;

/**
 * @author Alexander Reshetnyak
 */
@RolesAllowed({})
public class WorkspacesStatisticsPrecomputed extends AbstractCount implements PrecomputedMetric {


    public WorkspacesStatisticsPrecomputed() {
        super(MetricType.WORKSPACES_STATISTICS_PRECOMPUTED,
              MetricType.WORKSPACES_STATISTICS_LIST_PRECOMPUTED,
              WS);
    }

    @Override
    public String getDescription() {
        return "The number of workspaces in statistics";
    }

    @Override
    public boolean canReadPrecomputedData(Context context) {
        return ((PrecomputedMetric)MetricFactory.getMetric(MetricType.WORKSPACES_STATISTICS_LIST_PRECOMPUTED)).canReadPrecomputedData(context);
    }
}