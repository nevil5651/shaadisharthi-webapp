import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ToastrModule } from 'ngx-toastr';

import { VerifyEmailMessageComponent } from './verify-email-message';

describe('VerifyEmailMessageComponent', () => {
  let component: VerifyEmailMessageComponent;
  let fixture: ComponentFixture<VerifyEmailMessageComponent>;

  // Mock ActivatedRoute with fake query params
  const fakeActivatedRoute = {
    snapshot: {
      queryParamMap: convertToParamMap({ token: 'mock-token' })
    },
    queryParamMap: of(convertToParamMap({ token: 'mock-token' }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        VerifyEmailMessageComponent,
        HttpClientTestingModule,
        ToastrModule.forRoot()
      ],
      providers: [
        { provide: ActivatedRoute, useValue: fakeActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyEmailMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});