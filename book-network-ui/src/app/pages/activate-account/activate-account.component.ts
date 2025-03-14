import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthenticationService } from '../../services/services';
import { CommonModule } from '@angular/common';
import { CodeInputModule } from 'angular-code-input';

@Component({
  selector: 'app-activate-account',
  imports: [CommonModule, CodeInputModule],
  templateUrl: './activate-account.component.html',
  styleUrl: './activate-account.component.scss'
})
export class ActivateAccountComponent {
  message: string = "";
  isOkay: boolean = true;
  submitted: boolean = false;

  constructor(
    private router: Router,
    private authService: AuthenticationService
  ) {}

  private confirmAccount(token: string) {
    this.authService.confirm({
      token: token
    }).subscribe({
      next: () => {
        this.message = "Account Activated Successfully";
        this.submitted = true;
        this.isOkay = true;
      },
      error: () => {
        this.message = "Account Activation Failed or Token Expired";
        this.submitted = true;
        this.isOkay = false;
      }
    })
  }

  redirectToLogin() {
    this.router.navigate(['login']);
  };
 
  onCodeCompleted(token: string) {
    this.confirmAccount(token);
  }
}
