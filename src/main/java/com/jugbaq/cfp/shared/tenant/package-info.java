@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = java.util.UUID.class)
)
package com.jugbaq.cfp.shared.tenant;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;