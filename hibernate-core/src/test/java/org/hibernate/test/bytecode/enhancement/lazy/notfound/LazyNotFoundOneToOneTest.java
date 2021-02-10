/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.notfound;

/**
 * @author Gail Badner
 */

import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-12226")
@RunWith( BytecodeEnhancerRunner.class )
public class LazyNotFoundOneToOneTest extends BaseCoreFunctionalTestCase {
	private static int ID = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Lazy.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
		configuration.setProperty( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Test
	@FailureExpected(
			jiraKey = "HHH-13658",
			message = "Assertions specific to enhanced lazy loading but disallowing enhanced proxies, which is no longer valid"
	)
	public void test() {
		doInHibernate(
				this::sessionFactory, session -> {
					Lazy p = new Lazy();
					p.id = ID;
					User u = new User();
					u.id = ID;
					u.setLazy( p );
					session.persist( u );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( session.get( Lazy.class, ID ) );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					User user = session.find( User.class, ID );
					assertFalse( Hibernate.isPropertyInitialized( user, "lazy" ) );
					assertNull( user.getLazy() );
				}
		);
	}

	@Entity(name="User")
	@Table(name = "USER_TABLE")
	public static class User {

		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(value = LazyToOneOption.NO_PROXY)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private Lazy lazy;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Lazy getLazy() {
			return lazy;
		}

		public void setLazy(Lazy lazy) {
			this.lazy = lazy;
		}

	}

	@Entity(name = "Lazy")
	@Table(name = "LAZY")
	public static class Lazy {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
