package com.codehacks.blog.archtests;

import com.codehacks.blog.BlogAppApplication;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packagesOf = BlogAppApplication.class)
public class BlogAppArchitectureTests {

    @ArchTest
    public static final ArchRule controllerNaming = classes().that()
            .areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller");


    @ArchTest
    public static final ArchRule serviceNaming = classes().that()
            .areAnnotatedWith(Service.class)
            .should().haveSimpleNameContaining("Service");


    @ArchTest
    public static final ArchRule serviceShouldImplementInterface = classes().that()
            .areAnnotatedWith(Service.class)
            .should(new ArchCondition<JavaClass>("implement an interface") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean implementsInterface = !item.getInterfaces().isEmpty();
                    events.add(new SimpleConditionEvent(item, implementsInterface,
                            String.format("Class %s does not implement any interface", item.getName())));
                }
            })
            .because("Service classes should implement an interface for better abstraction and testability");


    @ArchTest
    public static final ArchRule repositoryNaming = classes().that()
            .areAnnotatedWith(Repository.class)
            .should().haveSimpleNameEndingWith("Repository");


    @ArchTest
    public static final ArchRule exceptionNaming = classes().that()
            .areAssignableTo(RuntimeException.class)
            .and().areNotAnonymousClasses()
            .should().haveSimpleNameEndingWith("Exception");

    @ArchTest
    public static final ArchRule repositoriesShouldOnlyBeAccessedByServices = classes().that()
            .areAnnotatedWith(Repository.class)
            .should().onlyBeAccessed().byAnyPackage("..repository..", "..service..")
            .because("Services should only be accessed by services");

}