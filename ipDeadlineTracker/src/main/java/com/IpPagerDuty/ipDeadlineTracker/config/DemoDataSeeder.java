package com.IpPagerDuty.ipDeadlineTracker.config;

import com.IpPagerDuty.ipDeadlineTracker.domain.Organization;
import com.IpPagerDuty.ipDeadlineTracker.domain.OrganizationMember;
import com.IpPagerDuty.ipDeadlineTracker.domain.User;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.OrganizationMemberRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.OrganizationRepository;
import com.IpPagerDuty.ipDeadlineTracker.domain.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DemoDataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;

    public DemoDataSeeder(UserRepository users, OrganizationRepository organizations, OrganizationMemberRepository members) {
        this.users = users;
        this.organizations = organizations;
        this.members = members;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Organization organization = organizations.findAll().stream()
            .filter(item -> "IPPagerDuty Demo".equals(item.getName()))
            .findFirst()
            .orElseGet(() -> {
                Organization created = new Organization();
                created.setName("IPPagerDuty Demo");
                created.setReminderOffsetsDays("30,7,2,1");
                return organizations.save(created);
            });

        addMember(organization, "owner@demo.local", "Demo Owner", OrganizationMember.Role.BUSINESS_OWNER);
        addMember(organization, "lawyer@demo.local", "Demo Lawyer", OrganizationMember.Role.LAWYER);
        addMember(organization, "paralegal@demo.local", "Demo Paralegal", OrganizationMember.Role.PARALEGAL);
        addMember(organization, "client@demo.local", "Demo Client", OrganizationMember.Role.CLIENT);
    }

    private void addMember(Organization organization, String email, String displayName, OrganizationMember.Role role) {
        User user = users.findByEmail(email).orElseGet(() -> {
            User created = new User();
            created.setEmail(email);
            created.setDisplayName(displayName);
            return users.save(created);
        });
        if (members.findByUserEmail(email).isEmpty()) {
            OrganizationMember member = new OrganizationMember();
            member.setOrganization(organization);
            member.setUser(user);
            member.setRole(role);
            members.save(member);
        }
    }
}
