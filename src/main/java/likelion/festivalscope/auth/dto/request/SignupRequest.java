package likelion.festivalscope.auth.dto.request;
import jakarta.validation.constraints.*;
public record SignupRequest(@NotBlank @Email String email, @NotBlank @Size(min=8, max=100) String password, @NotBlank @Size(max=100) String name) {}
