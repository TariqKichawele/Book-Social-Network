import { Component } from '@angular/core';
import type { AuthenticationRequest } from '../../services/models';
import { Router } from '@angular/router';
import { AuthenticationService } from '../../services/services';
import { CommonModule } from '@angular/common';
import { TokenService } from '../../services/token/token.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  authRequest: AuthenticationRequest = {
    email: '',
    password: ''
  };

  constructor(
    private router: Router,
    private authSerivce: AuthenticationService,
    private tokenService: TokenService
  ) {}

  errorMsg: Array<string> = [];

  login() {
    this.errorMsg = [];

    this.authSerivce.authenticate({
      body: this.authRequest
    }).subscribe({
      next: (res) => {
        this.tokenService.token = res.token as string; 
        this.router.navigate(['books']);
      },
      error: (err) => {
        console.log(err);
        if (err.error.validationErrors) {
          this.errorMsg = err.error.validationErrors;
        } else {
          this.errorMsg.push(err.error.message);
        }
      }
    })
  }

  register() {
    this.router.navigate(['register']);
  }
}
