/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.rrm.ehour.ui.common.renderers;

import net.rrm.ehour.domain.UserRole;
import net.rrm.ehour.ui.common.util.WebUtils;

public class UserRoleRenderer extends LocalizedDomainObjectResourceRenderer<UserRole> {
    private static final long serialVersionUID = -4020935210828625185L;

    @Override
    protected String getResourceKey(UserRole role) {
        return WebUtils.getResourceKeyForUserRole(role);
    }

}
