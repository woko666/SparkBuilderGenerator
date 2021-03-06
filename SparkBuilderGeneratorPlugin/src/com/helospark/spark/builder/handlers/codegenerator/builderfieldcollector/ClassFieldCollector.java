package com.helospark.spark.builder.handlers.codegenerator.builderfieldcollector;

import static com.helospark.spark.builder.preferences.PluginPreferenceList.INCLUDE_VISIBLE_FIELDS_FROM_SUPERCLASS;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.helospark.spark.builder.handlers.codegenerator.component.helper.ApplicableFieldVisibilityFilter;
import com.helospark.spark.builder.handlers.codegenerator.component.helper.FieldNameToBuilderFieldNameConverter;
import com.helospark.spark.builder.handlers.codegenerator.component.helper.TypeDeclarationFromSuperclassExtractor;
import com.helospark.spark.builder.handlers.codegenerator.domain.BuilderField;
import com.helospark.spark.builder.handlers.codegenerator.domain.ClassFieldSetterBuilderField;
import com.helospark.spark.builder.preferences.PreferencesManager;

/**
 * Collects the field parameters.
 * @author helospark
 */
public class ClassFieldCollector {
    private FieldNameToBuilderFieldNameConverter fieldNameToBuilderFieldNameConverter;
    private PreferencesManager preferencesManager;
    private TypeDeclarationFromSuperclassExtractor typeDeclarationFromSuperclassExtractor;
    private ApplicableFieldVisibilityFilter applicableFieldVisibilityFilter;

    public ClassFieldCollector(FieldNameToBuilderFieldNameConverter fieldNameToBuilderFieldNameConverter, PreferencesManager preferencesManager,
            TypeDeclarationFromSuperclassExtractor typeDeclarationFromSuperclassExtractor, ApplicableFieldVisibilityFilter applicableFieldVisibilityFilter) {
        this.fieldNameToBuilderFieldNameConverter = fieldNameToBuilderFieldNameConverter;
        this.preferencesManager = preferencesManager;
        this.typeDeclarationFromSuperclassExtractor = typeDeclarationFromSuperclassExtractor;
        this.applicableFieldVisibilityFilter = applicableFieldVisibilityFilter;
    }

    public List<? extends BuilderField> findBuilderFieldsRecursively(TypeDeclaration originalOwnerClasss, TypeDeclaration currentOwnerClass) {
        List<BuilderField> builderFields = new ArrayList<>();

        if (preferencesManager.getPreferenceValue(INCLUDE_VISIBLE_FIELDS_FROM_SUPERCLASS)) {
            builderFields.addAll(getFieldsFromSuperclass(currentOwnerClass));
        }

        FieldDeclaration[] fields = currentOwnerClass.getFields();
        for (FieldDeclaration field : fields) {
            List<VariableDeclarationFragment> fragments = field.fragments();
            builderFields.addAll(getFilteredDeclarations(field, fragments));
        }
        return builderFields;
    }

    private List<BuilderField> getFieldsFromSuperclass(TypeDeclaration currentTypeDeclaration) {
        return typeDeclarationFromSuperclassExtractor.extractTypeDeclarationFromSuperClass(currentTypeDeclaration)
                .map(parentTypeDeclaration -> findBuilderFieldsRecursively(currentTypeDeclaration, parentTypeDeclaration))
                .map(fields -> applicableFieldVisibilityFilter.filterSuperClassFieldsToVisibleFields(fields, currentTypeDeclaration))
                .orElse(emptyList());
    }

    private List<BuilderField> getFilteredDeclarations(FieldDeclaration field, List<VariableDeclarationFragment> fragments) {
        return fragments.stream()
                .filter(variableFragment -> !isStatic(field))
                .map(variableFragment -> createNamedVariableDeclarations(variableFragment, field))
                .collect(Collectors.toList());
    }

    private BuilderField createNamedVariableDeclarations(VariableDeclarationFragment variableDeclarationFragment, FieldDeclaration fieldDeclaration) {
        String originalFieldName = variableDeclarationFragment.getName().toString();
        String builderFieldName = fieldNameToBuilderFieldNameConverter.convertFieldName(originalFieldName);
        return ClassFieldSetterBuilderField.builder()
                .withFieldType(fieldDeclaration.getType())
                .withFieldDeclaration(fieldDeclaration)
                .withOriginalFieldName(originalFieldName)
                .withBuilderFieldName(builderFieldName)
                .build();
    }

    private boolean isStatic(FieldDeclaration field) {
        List<IExtendedModifier> fieldModifiers = field.modifiers();
        return fieldModifiers.stream()
                .filter(modifier -> modifier instanceof Modifier)
                .filter(modifer -> ((Modifier) modifer).getKeyword().equals(ModifierKeyword.STATIC_KEYWORD))
                .findFirst()
                .isPresent();
    }
}
