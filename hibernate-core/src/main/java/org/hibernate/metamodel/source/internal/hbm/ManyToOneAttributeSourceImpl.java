/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbColumnElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.SingularAttributeNature;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Implementation for {@code <many-to-one/>} mappings
 *
 * @author Steve Ebersole
 */
class ManyToOneAttributeSourceImpl extends AbstractToOneAttributeSourceImpl {
	private final JaxbManyToOneElement manyToOneElement;
	private final HibernateTypeSourceImpl typeSource;

	private final String containingTableName;
	private final List<RelationalValueSource> valueSources;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	ManyToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer container,
			final JaxbManyToOneElement manyToOneElement,
			final String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument, naturalIdMutability, manyToOneElement.getPropertyRef() );
		this.manyToOneElement = manyToOneElement;

		final String referencedClassName = manyToOneElement.getClazz();
		JavaTypeDescriptor referencedClass = null;
		if ( StringHelper.isNotEmpty( referencedClassName ) ) {
			referencedClass = bindingContext().getJavaTypeDescriptorRepository().getType(
					bindingContext().getJavaTypeDescriptorRepository().buildName(
							bindingContext().qualifyClassName( manyToOneElement.getClazz() )
					)
			);
		}
		this.typeSource = new HibernateTypeSourceImpl( referencedClass );

		this.containingTableName = logicalTableName;
		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return manyToOneElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToOneElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return manyToOneElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return manyToOneElement.getFormula();
					}

					@Override
					public String getContainingTableName() {
						return logicalTableName;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return manyToOneElement.isInsert();
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return manyToOneElement.isUpdate();
					}
				}
		);

		this.attributeRole = container.getAttributeRoleBase().append( manyToOneElement.getName() );
		this.attributePath = container.getAttributePathBase().append( manyToOneElement.getName() );
	}

	@Override
	public String getName() {
			return manyToOneElement.getName();
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public HibernateTypeSourceImpl getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneElement.getAccess();
	}

	@Override
	public boolean isIgnoreNotFound() {
		return manyToOneElement.getNotFound() != null && "ignore".equalsIgnoreCase( manyToOneElement.getNotFound().value() );
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return manyToOneElement.isOptimisticLock();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( manyToOneElement.getCascade(), bindingContext() );
	}

	@Override
	protected boolean requiresImmediateFetch() {
		return false;
	}

	@Override
	protected String getFetchSelectionString() {
		return manyToOneElement.getFetch() != null ?
				manyToOneElement.getFetch().value() :
				null;
	}

	@Override
	protected String getLazySelectionString() {
		return manyToOneElement.getLazy() != null ?
				manyToOneElement.getLazy().value() :
				null;
	}

	@Override
	protected String getOuterJoinSelectionString() {
		return manyToOneElement.getOuterJoin() != null ?
				manyToOneElement.getOuterJoin().value() :
				null;
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.MANY_TO_ONE;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return manyToOneElement.isInsert();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return manyToOneElement.isUpdate();
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return ! Helper.getValue( manyToOneElement.isNotNull(), false );
	}

	@Override
	public String getContainingTableName() {
		return containingTableName;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return manyToOneElement.getMeta();
	}

	@Override
	public String getReferencedEntityName() {
		return manyToOneElement.getClazz() != null
				? bindingContext().qualifyClassName( manyToOneElement.getClazz() )
				: manyToOneElement.getEntityName();
	}

	@Override
	public boolean isUnique() {
		return manyToOneElement.isUnique();
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToOneElement.getForeignKey();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

}
