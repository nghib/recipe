package ca.derekcormier.recipe.generator;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.derekcormier.recipe.cookbook.Cookbook;
import ca.derekcormier.recipe.cookbook.CookbookUtils;
import ca.derekcormier.recipe.cookbook.Ingredient;
import ca.derekcormier.recipe.cookbook.Optional;
import ca.derekcormier.recipe.cookbook.Param;
import ca.derekcormier.recipe.cookbook.Required;
import ca.derekcormier.recipe.cookbook.type.ArrayType;
import ca.derekcormier.recipe.cookbook.type.FlagType;
import ca.derekcormier.recipe.cookbook.type.ParamType;
import ca.derekcormier.recipe.cookbook.type.Type;
import liqp.filters.Filter;

public class TypescriptIngredientGenerator extends CookbookGenerator {
    @Override
    public void generate(Cookbook cookbook, String targetDir, Map<String, Object> options) {
        registerFilters(cookbook);
        String directory = createDirectories(targetDir);

        // generate javascript ingredients
        for (Ingredient ingredient: cookbook.getIngredients()) {
            Map<String,Object> info = new HashMap<>();
            info.put("requiredTypes", getRequiredTypeMapping(ingredient));
            info.put("nonPrimitiveTypes", getNonPrimitiveTypes(ingredient, cookbook));

            Map<String,Object> data = new HashMap<>();
            data.put("ingredient", ingredient);
            data.put("domain", cookbook.getDomain());
            data.put("options", options);
            data.put("info", info);
            String rendered = renderTemplate("templates/ts/ingredient.liquid", data);
            String filepath = directory + File.separator + ingredient.getName() + ".js";
            writeToFile(filepath, rendered);
        }

        // generate typescript definitions for ingredients
        for (Ingredient ingredient: cookbook.getIngredients()) {
            Map<String,Object> info = new HashMap<>();
            info.put("nonPrimitiveTypes", getNonPrimitiveTypes(ingredient, cookbook));

            Map<String,Object> data = new HashMap<>();
            data.put("ingredient", ingredient);
            data.put("domain", cookbook.getDomain());
            data.put("options", options);
            data.put("info", info);
            String rendered = renderTemplate("templates/ts/ingredient-types.liquid", data);
            String filepath = directory + File.separator + ingredient.getName() + ".d.ts";
            writeToFile(filepath, rendered);
        }

        // generate enum definitions
        for (ca.derekcormier.recipe.cookbook.Enum enumeration: cookbook.getEnums()) {
            Map<String,Object> data = new HashMap<>();
            data.put("enum", enumeration);
            data.put("options", options);
            String rendered = renderTemplate("templates/ts/enum-types.liquid", data);
            String filepath = directory + File.separator + enumeration.getName() + ".d.ts";
            writeToFile(filepath, rendered);
        }

        // generate enums
        for (ca.derekcormier.recipe.cookbook.Enum enumeration: cookbook.getEnums()) {
            Map<String,Object> data = new HashMap<>();
            data.put("enum", enumeration);
            data.put("options", options);
            String rendered = renderTemplate("templates/ts/enum.liquid", data);
            String filepath = directory + File.separator + enumeration.getName() + ".js";
            writeToFile(filepath, rendered);
        }

        // generate index definition file
        Map<String, Object> data = new HashMap<>();
        data.put("ingredients", cookbook.getIngredients());
        data.put("enums", cookbook.getEnums());
        data.put("domain", cookbook.getDomain());
        data.put("options", options);
        String rendered = renderTemplate("templates/ts/ingredient-index-types.liquid", data);
        String filepath = directory + File.separator + "index.d.ts";
        writeToFile(filepath, rendered);

        // generate index file
        data = new HashMap<>();
        data.put("ingredients", cookbook.getIngredients());
        data.put("enums", cookbook.getEnums());
        data.put("domain", cookbook.getDomain());
        data.put("options", options);
        rendered = renderTemplate("templates/ts/ingredient-index.liquid", data);
        filepath = directory + File.separator + "index.js";
        writeToFile(filepath, rendered);
    }

    private void registerFilters(Cookbook cookbook) {
        Filter.registerFilter(TypescriptFilters.createTsTypeFilter(cookbook));
        Filter.registerFilter(TypescriptFilters.createTsParamFilter(cookbook));
        Filter.registerFilter(TypescriptFilters.createJsParamFilter(cookbook));
        Filter.registerFilter(TypescriptFilters.createTsValueFilter(cookbook));
    }

    private Map<String,String> getRequiredTypeMapping(Ingredient ingredient) {
        Map<String,String> requiredTypes = new HashMap<>();

        for (Required required: ingredient.getRequired()) {
            requiredTypes.put(required.getName(), required.getType());
        }

        return requiredTypes;
    }

    private Set<String> getNonPrimitiveTypes(Ingredient ingredient, Cookbook cookbook) {
        Set<String> types = new HashSet<>();
        for (Required required: ingredient.getRequired()) {
            if (isNonPrimitive(required.getType(), cookbook)) {
                types.add(getBaseType(required.getType(), cookbook));
            }
        }

        for (Optional optional: ingredient.getOptionals()) {
            if (!optional.isCompound() && isNonPrimitive(optional.getType(), cookbook)) {
                types.add(getBaseType(optional.getType(), cookbook));
            }
            else if (optional.isCompound()) {
                for (Param param: optional.getParams()) {
                    if (isNonPrimitive(param.getType(), cookbook)) {
                        types.add(getBaseType(param.getType(), cookbook));
                    }
                }
            }
        }

        return types;
    }

    private boolean isNonPrimitive(String type, Cookbook cookbook) {
        Type t = CookbookUtils.parseType(type, cookbook).getType();
        return !CookbookUtils.isPrimitiveType(t) && !(t instanceof FlagType);
    }

    private String getBaseType(String type, Cookbook cookbook) {
        ParamType paramType = CookbookUtils.parseType(type, cookbook);

        if (paramType.getType() instanceof ArrayType) {
            return getBaseType(((ArrayType)paramType.getType()).getBaseType().name(), cookbook);
        }
        else {
            return paramType.getType().name();
        }
    }
}