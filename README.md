# spring-data-eclipselink-multitenancy

Allows to use Spring Data with EclipseLink single-table multitenancy.

Using multitenancy with <a href="http://www.eclipse.org/eclipselink">EclipseLink</a> is quite straightforward using @MultiTenant annotation.

However, there are some limitations with default behavior that this project allows to overcome :
- The persistence units must be defined at configuration level and cannot be created dynamically out of the box. One solution is to use a single persistence unit for all tenants (argh!).
- If you decide to go for a single persistence unit for all tenants, second level cache is not an option whenever you do something else than findById()

This project allows to :
- Create dynamically a persistence unit whenever a call on a new tenant is made
- Use separate PU for each tenant which allow to use second level cache and avoid mixing data
- Code using standard spring-data-jpa repositories
