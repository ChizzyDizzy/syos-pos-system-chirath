package com.syos.application.commands.user;

import com.syos.application.services.UserService;
import com.syos.infrastructure.ui.presenters.UserPresenter;
import com.syos.infrastructure.ui.cli.InputReader;
import com.syos.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LoginCommandTest {

    @Mock
    private UserService userService;

    @Mock
    private UserPresenter presenter;

    @Mock
    private InputReader inputReader;

    @Mock
    private User mockUser;

    private LoginCommand loginCommand;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginCommand = new LoginCommand(userService, presenter, inputReader);
    }

    @Test
    void should_login_successfully_and_show_user_permissions_when_valid_credentials_provided() {
        // Arrange
        String username = "testuser";
        String password = "testpass";

        // Mock user not already logged in
        when(userService.isLoggedIn()).thenReturn(false);

        // Mock input reading
        when(inputReader.readString("Username: ")).thenReturn(username);
        when(inputReader.readString("")).thenReturn(password);

        // Mock successful login
        when(userService.login(username.toLowerCase(), password)).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(mockUser);

        // Mock user permissions
        when(mockUser.getUsername()).thenReturn(username);
        when(mockUser.canProcessSales()).thenReturn(true);
        when(mockUser.canManageInventory()).thenReturn(false);
        when(mockUser.canViewReports()).thenReturn(true);
        when(mockUser.canManageUsers()).thenReturn(false);

        // Act
        loginCommand.execute();

        // Assert
        verify(presenter).showHeader("User Login");
        verify(userService).login(username.toLowerCase(), password);
        verify(presenter).showLoginSuccess(mockUser);
        verify(userService).getCurrentUser();

        // Verify user permissions were checked
        verify(mockUser).canProcessSales();
        verify(mockUser).canManageInventory();
        verify(mockUser).canViewReports();
        verify(mockUser).canManageUsers();

        // Verify no error messages
        verify(presenter, never()).showError(anyString());
    }

    @Test
    void should_deny_access_after_maximum_login_attempts_exceeded() {
        // Arrange
        String invalidUsername = "wronguser";
        String invalidPassword = "wrongpass";

        // Mock user not already logged in
        when(userService.isLoggedIn()).thenReturn(false);

        // Mock input reading - same invalid credentials for all attempts
        when(inputReader.readString("Username: ")).thenReturn(invalidUsername);
        when(inputReader.readString("")).thenReturn(invalidPassword);

        // Mock failed login attempts
        when(userService.login(invalidUsername.toLowerCase(), invalidPassword)).thenReturn(false);

        // Act
        loginCommand.execute();

        // Assert
        verify(presenter).showHeader("User Login");

        // Verify exactly 3 login attempts were made
        verify(userService, times(3)).login(invalidUsername.toLowerCase(), invalidPassword);

        // Verify error messages for each failed attempt
        verify(presenter).showError("Invalid username or password. 2 attempts remaining.");
        verify(presenter).showError("Invalid username or password. 1 attempts remaining.");
        verify(presenter).showError("Maximum login attempts exceeded. Access denied.");
        verify(presenter).showError("Please contact system administrator.");

        // Verify no successful login occurred
        verify(presenter, never()).showLoginSuccess(any(User.class));
        verify(userService, never()).getCurrentUser();
    }

}