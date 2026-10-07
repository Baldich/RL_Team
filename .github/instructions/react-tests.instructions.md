---
applyTo: "frontend/**/*.test.tsx"
---

# React Testing Guidelines

- Query elements primarily by role using Testing Library (`getByRole`, `findByRole`).
- Mock all API endpoints; never invoke real backend services in unit tests.
- Maintain one `describe` block per component.
- Ensure all asynchronous state updates are properly handled with `waitFor` or `findBy*`.
- Run tests locally before committing: `npm test`.