// Wrapper for Bootstrap form validation in React
// Adds client-side validation classes on submit

import React, { useRef, useEffect } from "react";

/**
 * ValidatedForm - HOC-like component for form validation
 * @param {Function} onSubmit - Submit handler
 * @param {ReactNode} children - Form fields
 * @param {string} className - Additional classes
 * @param {Object} props - Other form props
 */
const ValidatedForm = ({ onSubmit, children, className = "", ...props }) => {
  const formRef = useRef(null); // Ref to access DOM form element

  // Effect to attach validation on mount
  useEffect(() => {
    const form = formRef.current;
    if (!form) return; // Early return if ref not set

    // Submit listener: Check validity, add validation class
    const handleSubmit = (event) => {
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation(); // Prevent submission if invalid
      }
      form.classList.add("was-validated"); // Trigger Bootstrap validation UI
    };

    form.addEventListener("submit", handleSubmit);

    // Cleanup: Remove listener on unmount
    return () => {
      form.removeEventListener("submit", handleSubmit);
    };
  }, []); // Empty deps: Runs once on mount

  return (
    <form
      ref={formRef}
      className={`needs-validation ${className}`} // Bootstrap validation classes
      noValidate // Disable native HTML validation (use Bootstrap)
      onSubmit={onSubmit}
      {...props}
    >
      {children}
    </form>
  );
};

export default ValidatedForm;
