/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.machine.server.jpa;

import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.jpa.JpaRecipeDao;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.RecipeDao;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * JPA {@link RecipeDao} implementation that respects permissions on search by user.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 */
@Singleton
public class OnPremisesJpaRecipeDao extends JpaRecipeDao {

    @Inject
    private Provider<EntityManager> managerProvider;

    private static final String findByPermissionsAndTypeQuery = "SELECT recipe FROM RecipePermissions perm" +
                                                                "       LEFT JOIN perm.recipe recipe  " +
                                                                "       WHERE (perm.userId IS NULL OR perm.userId  = :userId)" +
                                                                "       AND 'search' MEMBER OF perm.actions " +
                                                                "       AND (recipe.type IS NULL OR recipe.type = :recipeType) ";

    private static final String findByPermissionsTagsAndTypeQuery = "SELECT recipe FROM RecipePermissions perm" +
                                                                    "       LEFT JOIN perm.recipe recipe  " +
                                                                    "       LEFT JOIN recipe.tags tag     " +
                                                                    "       WHERE (perm.userId IS NULL OR perm.userId  = :userId)" +
                                                                    "       AND 'search' MEMBER OF perm.actions " +
                                                                    "       AND (recipe.type IS NULL OR recipe.type = :recipeType) " +
                                                                    "       AND tag IN :tags " +
                                                                    "       GROUP BY recipe.id HAVING COUNT(tag) = :tagsSize";

    @Override
    @Transactional
    public List<RecipeImpl> search(String userId,
                                   List<String> tags,
                                   String type,
                                   int skipCount,
                                   int maxItems) throws ServerException {

        final TypedQuery<RecipeImpl> query;
        if (tags == null || tags.isEmpty()) {
            query = managerProvider.get().createQuery(findByPermissionsAndTypeQuery, RecipeImpl.class);
        } else {
            query = managerProvider.get()
                                   .createQuery(findByPermissionsTagsAndTypeQuery, RecipeImpl.class)
                                   .setParameter("tags", tags)
                                   .setParameter("tagsSize", tags.size());
        }
        try {
            return query.setParameter("userId", userId)
                        .setParameter("recipeType", type)
                        .setFirstResult(skipCount)
                        .setMaxResults(maxItems)
                        .getResultList();
        } catch (RuntimeException ex) {
            throw new ServerException(ex.getLocalizedMessage(), ex);
        }
    }
}
